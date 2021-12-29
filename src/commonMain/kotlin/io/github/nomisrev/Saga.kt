package io.github.nomisrev

import io.github.nomisrev.internal.AtomicRef
import io.github.nomisrev.internal.updateAndGet
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * The saga design pattern is a way to manage data consistency across microservices in distributed
 * transaction scenarios. A [Saga] is useful when you need to manage data in a consistent manner
 * across services in distributed transaction scenarios. Or when you need to compose multiple
 * `actions` with a `compensation` that needs to run in a transaction like style.
 *
 * For example, let's say that we have the following domain types `Order`, `Payment`.
 *
 * ```kotlin
 * data class Order(val id: UUID, val amount: Long)
 * data class Payment(val id: UUID, val orderId: UUID)
 * ```
 *
 * The creation of an `Order` can only remain when a payment has been made. In SQL, you might run
 * this inside a transaction, which can automatically roll back the creation of the `Order` when the
 * creation of the Payment fails.
 *
 * When you need to do this across distributed services, or a multiple atomic references, etc. You
 * need to manually facilitate the rolling back of the performed actions, or compensating actions.
 *
 * The [Saga] type, and [saga] DSL remove all the boilerplate of manually having to facilitate this
 * with a convenient suspending DSL.
 *
 * ```kotlin
 * data class Order(val id: UUID, val amount: Long)
 * suspend fun createOrder(): Order = Order(UUID.randomUUID(), 100L)
 * suspend fun deleteOrder(order: Order): Unit = println("Deleting $order")
 *
 * data class Payment(val id: UUID, val orderId: UUID)
 * suspend fun createPayment(order: Order): Payment = Payment(UUID.randomUUID(), order.id)
 * suspend fun deletePayment(payment: Payment): Unit = println("Deleting $payment")
 *
 * suspend fun Payment.awaitSuccess(): Unit = throw RuntimeException("Payment Failed")
 *
 * suspend fun main() {
 *   saga {
 *     val order = saga { createOrder() }.compensate(::deleteOrder).bind()
 *     val payment = saga { createPayment(order) }.compensate(::deletePayment).bind()
 *     payment.awaitSuccess()
 *   }.transact()
 * }
 * ```
 */
public class Saga<A>(
  public val action: suspend SagaEffect.() -> A,
  public val compensation: suspend (A) -> Unit
) {

  /**
   * Add a compensating action to a [Saga]. A single [Saga] can have many compensating actions, they
   * will be composed in a FILO order. This makes sure they're executed in reverse order as the
   * actions.
   *
   * ```kotlin
   * saga {
   *   saga { println("A") }
   *     .compensate { println("A - 1") }
   *     .compensate { println("A - 2") }
   *     .bind
   *   throw RuntimeException("Boom!")
   * }.transact()
   * // A - 2
   * // A - 1
   * // RuntimeException("Boom!")
   * ```
   */
  public infix fun compensate(compensate: suspend (A) -> Unit): Saga<A> =
    Saga(action) { a ->
      compensation(a)
      compensate(a)
    }

  /**
   * Transact runs the [Saga] turning it into a [suspend] effect that results in [A]. If the saga
   * fails then all compensating actions are guaranteed to run. When a compensating action failed it
   * will be ignored, and the other compensating actions will continue to be run.
   */
  public suspend fun transact(): A {
    val builder = SagaBuilder()
    return guaranteeCase({ action(builder) }) { exitCase ->
      when (exitCase) {
        is ExitCase.Completed -> Unit
        else -> builder.totalCompensation()
      }
    }
  }
}

/** Receiver DSL of the `saga { }` builder. */
public interface SagaEffect {

  /** Runs a [Saga] and registers it's `compensation` task after the `action` finishes running */
  public suspend fun <A> Saga<A>.bind(): A
}

/**
 * The Saga builder which exposes the [SagaEffect.bind]. The `saga` builder uses the suspension
 * system to run actions, and automatically register their compensating actions.
 *
 * When the resulting [Saga] fails it will run all the required compensating actions, also when the
 * [Saga] gets cancelled it will respect its compensating actions before returning.
 *
 * By doing so we can guarantee that any transactional like operations made by the [Saga] will
 * guarantee that it results in the correct state.
 */
public fun <A> saga(block: suspend SagaEffect.() -> A): Saga<A> = Saga(block) {}

/**
 * Traverses the [Iterable] and composes all [Saga] returned by the applies [transform] function.
 *
 * ```kotlin
 * data class Order(val id: UUID, val amount: Long)
 * suspend fun updateOrder(inc: Long): Order = Order(UUID.randomUUID(), 100L + inc)
 * suspend fun reverseOrder(uuid: UUID, inc: Long): Unit = println("Decrementing order with $uuid with $inc")
 *
 * listOf(1, 2, 3).traverseSage { amount ->
 *   saga { updateOrder(amount) }.compensate { order -> reverseOrder(order.uuid, amount) }
 * }.transact()
 * ```
 */
public fun <A, B> Iterable<A>.mapSaga(transform: (a: A) -> Saga<B>): Saga<List<B>> =
  if (this is Collection && this.isEmpty()) Saga({ emptyList() }) {}
  else saga { map { transform(it).bind() } }

public fun <A, B> Iterable<A>.traverse(transform: (a: A) -> Saga<B>): Saga<List<B>> =
  mapSaga(transform)

/**
 * Alias for traverseSage { it }. Handy when you need to process `List<Saga<A>>` that might be
 * coming from another layer.
 *
 * i.e. when the database layer passes a `List<Saga<User>>` to the service layer, to abstract over
 * the database layer/DTO models since you might not be able to access those mappers from the whole
 * app.
 */
public fun <A> Iterable<Saga<A>>.sequence(): Saga<List<A>> =
  if (this is Collection && isEmpty()) Saga({ emptyList() }) {} else saga { map { it.bind() } }

// Internal implementation of the `saga { }` builder.
@PublishedApi
internal class SagaBuilder(
  private val stack: AtomicRef<List<suspend () -> Unit>> = AtomicRef(emptyList())
) : SagaEffect {

  override suspend fun <A> Saga<A>.bind(): A =
    guaranteeCase({ action() }) { exitCase ->
      when (exitCase) {
        is ExitCase.Completed<A> ->
          stack.updateAndGet { listOf(suspend { compensation(exitCase.value) }) + it }
        // This action failed, so we have no compensate to push on the stack
        // the compensation stack will run in the `transact` stage, this is just the builder
        is ExitCase.Cancelled -> Unit
        is ExitCase.Failure -> Unit
      }
    }

  @PublishedApi
  internal suspend fun totalCompensation() {
    stack
      .get()
      .mapNotNull { finalizer ->
        try {
          finalizer()
          null
        } catch (e: @Suppress("TooGenericExceptionCaught") Throwable) {
          e
        }
      }
      .reduceOrNull { acc, throwable -> acc.apply { addSuppressed(throwable) } }
      ?.let { throw it }
  }
}

internal sealed class ExitCase<out A> {
  data class Completed<A>(val value: A) : ExitCase<A>() {
    override fun toString(): String = "ExitCase.Completed"
  }

  data class Cancelled(val exception: CancellationException) : ExitCase<Nothing>()
  data class Failure(val failure: Throwable) : ExitCase<Nothing>()
}

@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
private suspend fun <A> guaranteeCase(
  fa: suspend () -> A,
  finalizer: suspend (ExitCase<A>) -> Unit
): A {
  val res =
    try {
      fa()
    } catch (e: CancellationException) {
      runReleaseAndRethrow(e) { finalizer(ExitCase.Cancelled(e)) }
    } catch (t: @Suppress("TooGenericExceptionCaught") Throwable) {
      runReleaseAndRethrow(t) { finalizer(ExitCase.Failure(t)) }
    }
  withContext(NonCancellable) { finalizer(ExitCase.Completed(res)) }
  return res
}

private suspend fun runReleaseAndRethrow(original: Throwable, f: suspend () -> Unit): Nothing {
  try {
    withContext(NonCancellable) { f() }
  } catch (e: @Suppress("TooGenericExceptionCaught") Throwable) {
    original.addSuppressed(e)
  }
  throw original
}
