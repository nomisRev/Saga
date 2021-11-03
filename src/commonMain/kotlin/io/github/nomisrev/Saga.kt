@file:Suppress("unused")

package io.github.nomisrev

import arrow.continuations.generic.AtomicRef
import arrow.continuations.generic.updateAndGet
import arrow.core.nonFatalOrThrow
import arrow.core.prependTo
import arrow.fx.coroutines.Platform
import arrow.fx.coroutines.parTraverse
import arrow.fx.coroutines.parZip
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * The saga design pattern is a way to manage data consistency across microservices in distributed
 * transaction scenarios. A [Saga] is useful when you need to manage data in a consistent manner
 * across services in distributed transaction scenarios. Or when you need to compose multiple
 * `actions` with a `compensation` that needs to run in a transaction like style.
 *
 * For example, let's say that we have following domain types `Order`, `Payment`.
 *
 * ```kotlin
 * data class Order(val id: UUID, val amount: Long)
 * data class Payment(val id: UUID, val orderId: UUID)
 * ```
 *
 * The creation of an `Order` can only remain when a payment has been made. In SQL you might run
 * this inside a transaction, which can automatically rollback the creation of the `Order` when the
 * creation of the Payment fails.
 *
 * When you need to do this across distributed services, or a multiple atomic references, etc you
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
public sealed interface Saga<A> {

  /**
   * Add a compensating action to a [Saga]. A single [Saga] can have many compensating actions, they
   * will be composed in a FILO order. This makes sure they're executed in reverse order as the
   * actions.
   *
   * ```kotlin
   * saga {
   *   saga { println("A") }.compensate { println("A - 1") }
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
    when (this) {
      is Builder -> Full({ f(this) }, compensate)
      is Full ->
        Full(action) { a ->
          compensate(a)
          this.compensation(a)
        }
      is Part -> Full(action, compensate)
    }

  /**
   * Transact runs the [Saga] turning it into a [suspend] effect that results in [A]. If the saga
   * fails then all compensating actions are guaranteed to run. When a compensating action failed it
   * will be ignored, and the other compensating actions will continue to be run.
   */
  public suspend fun transact(): A =
    when (this) {
      is Full -> saga { bind() }.transact()
      is Part -> saga { action(this) }.transact()
      is Builder -> _transact()
    }

  /**
   * Runs multiple [Saga]s in parallel and combines the result with the [transform] function. When
   * one of the two [Saga] fails then it will cancel the other, if the other [Saga] already finished
   * then its compensating action will be run.
   *
   * If the resulting Saga is cancelled, then all composed [Saga]s will also cancel. All actions
   * that already ran will get compensated first.
   */
  public fun <B, C> parZip(
    ctx: CoroutineContext,
    other: Saga<B>,
    transform: suspend CoroutineScope.(a: A, b: B) -> C
  ): Saga<C> = saga { parZip(ctx, { bind() }, { other.bind() }, transform) }

  public fun <B, C> parZip(
    other: Saga<B>,
    transform: suspend CoroutineScope.(a: A, b: B) -> C
  ): Saga<C> = parZip(Dispatchers.Default, other, transform)

  public fun <B, C, D> parZip(
    ctx: CoroutineContext,
    b: Saga<B>,
    c: Saga<C>,
    f: suspend CoroutineScope.(A, B, C) -> D
  ): Saga<D> = saga { parZip(ctx, { bind() }, { b.bind() }, { c.bind() }, f) }

  public fun <B, C, D, E> parZip(
    ctx: CoroutineContext,
    b: Saga<B>,
    c: Saga<C>,
    d: Saga<D>,
    f: suspend CoroutineScope.(A, B, C, D) -> E
  ): Saga<E> = saga { parZip(ctx, { bind() }, { b.bind() }, { c.bind() }, { d.bind() }, f) }

  public fun <B, C, D, E, F> parZip(
    ctx: CoroutineContext,
    b: Saga<B>,
    c: Saga<C>,
    d: Saga<D>,
    e: Saga<E>,
    f: suspend CoroutineScope.(A, B, C, D, E) -> F
  ): Saga<F> = saga {
    parZip(ctx, { bind() }, { b.bind() }, { c.bind() }, { d.bind() }, { e.bind() }, f)
  }

  public fun <B, C, D, E, F, G> parZip(
    ctx: CoroutineContext,
    b: Saga<B>,
    c: Saga<C>,
    d: Saga<D>,
    e: Saga<E>,
    ff: Saga<F>,
    f: suspend CoroutineScope.(A, B, C, D, E, F) -> G
  ): Saga<G> = saga {
    parZip(
      ctx,
      { bind() },
      { b.bind() },
      { c.bind() },
      { d.bind() },
      { e.bind() },
      { ff.bind() },
      f
    )
  }

  public fun <B, C, D, E, F, G, H> parZip(
    ctx: CoroutineContext,
    b: Saga<B>,
    c: Saga<C>,
    d: Saga<D>,
    e: Saga<E>,
    ff: Saga<F>,
    g: Saga<G>,
    f: suspend CoroutineScope.(A, B, C, D, E, F, G) -> H
  ): Saga<H> = saga {
    parZip(
      ctx,
      { bind() },
      { b.bind() },
      { c.bind() },
      { d.bind() },
      { e.bind() },
      { ff.bind() },
      { g.bind() },
      f
    )
  }

  public fun <B, C, D, E, F, G, H, I> parZip(
    ctx: CoroutineContext,
    b: Saga<B>,
    c: Saga<C>,
    d: Saga<D>,
    e: Saga<E>,
    ff: Saga<F>,
    g: Saga<G>,
    h: Saga<H>,
    f: suspend CoroutineScope.(A, B, C, D, E, F, G, H) -> I
  ): Saga<I> = saga {
    parZip(
      ctx,
      { bind() },
      { b.bind() },
      { c.bind() },
      { d.bind() },
      { e.bind() },
      { ff.bind() },
      { g.bind() },
      { h.bind() },
      f
    )
  }

  /**
   * A partial [Saga], tt only defines the [action].
   * @see Full for a [Saga] that defines both [action] and [Full.compensation].
   */
  @JvmInline
  public value class Part<A>(public val action: suspend SagaEffect.() -> A) : Saga<A> {
    override infix fun compensate(compensate: suspend (A) -> Unit): Saga<A> =
      Full(action, compensate)
  }

  /** Full for a [Saga] that defines both [action] and [Full.compensation]. */
  public class Full<A>(
    public val action: suspend SagaEffect.() -> A,
    public val compensation: suspend (A) -> Unit
  ) : Saga<A>

  /**
   * Wrapper around the `saga { }` builder. This was we can run the [Saga] on a single [SagaBuilder]
   * .
   */
  @JvmInline
  public value class Builder<A>(public val f: suspend SagaEffect.() -> A) : Saga<A> {
    @Suppress("FunctionName")
    internal suspend fun _transact(): A {
      val builder = SagaBuilder()
      return guaranteeCase({ f(builder) }) { exitCase ->
        when (exitCase) {
          is ExitCase.Completed -> Unit
          else -> builder.totalCompensation()
        }
      }
    }
  }
}

/** Receiver DSL of the `saga { }` builder. */
public interface SagaEffect {

  /** Runs a [Saga] and registers it's `compensation` task after the `action` finishes running */
  public suspend fun <A> Saga<A>.bind(): A

  /** Nested syntax for optimisation. */
  public fun <A> saga(action: suspend SagaEffect.() -> A): Saga.Part<A> = Saga.Part(action)
}

/**
 * The Saga builder which exposes the [SagaEffect.bind] and [SagaEffect.saga] API. The `saga`
 * builder uses the suspension system to run actions, and automatically register their compensating
 * actions.
 *
 * When the resulting [Saga] fails it will run all the required compensating actions, also when the
 * [Saga] gets cancelled it will respect its compensating actions before returning.
 *
 * By doing so we can guarantee that any transactional like operations made by the [Saga] will
 * guarantee that it results in the correct state.
 */
public fun <A> saga(block: suspend SagaEffect.() -> A): Saga<A> = Saga.Builder(block)

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
public fun <A, B> Iterable<A>.traverseSaga(transform: (a: A) -> Saga<B>): Saga<List<B>> =
  if (this is Collection && this.isEmpty()) Saga.Part { emptyList() }
  else saga { map { transform(it).bind() } }

/**
 * Alias for traverseSage { it }. Handy when you need to process `List<Saga<A>>` that might be
 * coming from another layer.
 *
 * i.e. when the database layer passes a `List<Saga<User>>` to the service layer, to abstract over
 * the database layer/DTO models since you might not be able to access those mappers from the whole
 * app.
 */
public fun <A> Iterable<Saga<A>>.sequence(): Saga<List<A>> =
  if (this is Collection && this.isEmpty()) Saga.Part { emptyList() }
  else saga { map { it.bind() } }

/**
 * Parallel version of [traverseSaga], it has the same semantics as [parZip] in terms of parallelism
 * and cancellation.
 *
 * When one of the two [Saga] fails then it will cancel the other, if the other [Saga] already
 * finished then its compensating action will be run.
 *
 * If the resulting Saga is cancelled, then all composed [Saga]s will also cancel. All actions that
 * already ran will get compensated first.
 */
public fun <A, B> Iterable<A>.parTraverseSaga(
  ctx: CoroutineContext,
  f: (A) -> Saga<B>
): Saga<List<B>> =
  if (this is Collection && this.isEmpty()) Saga.Part { emptyList() }
  else saga { parTraverse(ctx) { f(it).bind() } }

public fun <A, B> Iterable<A>.parTraverseSaga(f: (a: A) -> Saga<B>): Saga<List<B>> =
  parTraverseSaga(Dispatchers.Default, f)

/**
 * Alias for parTraverseSage { it }. Handy when you need to process `List<Saga<A>>` that might be
 * coming from another layer.
 *
 * i.e. when the database layer passes a `List<Saga<User>>` to the service layer, to abstract over
 * the database layer/DTO models since you might not be able to access those mappers from the whole
 * app.
 */
public fun <A> Iterable<Saga<A>>.parSequence(
  ctx: CoroutineContext = Dispatchers.Default
): Saga<List<A>> =
  if (this is Collection && this.isEmpty()) Saga.Part { emptyList() }
  else saga { parTraverse(ctx) { it.bind() } }

// Internal implementation of the `saga { }` builder.
@PublishedApi
internal class SagaBuilder(
  private val stack: AtomicRef<List<suspend () -> Unit>> = AtomicRef(emptyList())
) : SagaEffect {

  override suspend fun <A> Saga<A>.bind(): A =
    when (this) {
      is Saga.Full -> bind()
      is Saga.Part -> action()
      is Saga.Builder -> bind()
    }

  private suspend fun <A> Saga.Full<A>.bind() =
    guaranteeCase({ action() }) { exitCase ->
      when (exitCase) {
        is ExitCase.Completed<A> ->
          stack.updateAndGet { suspend { compensation(exitCase.value) } prependTo it }
        // This action failed so we have no compensate to push on the stack
        // the compensate stack will run in the `transact` stage, this is just the builder
        is ExitCase.Cancelled -> Unit
        is ExitCase.Failure -> Unit
      }
    }

  private suspend fun <A> Saga.Builder<A>.bind() = f(this@SagaBuilder)

  @PublishedApi
  internal suspend fun totalCompensation() {
    val errors =
      stack.get().mapNotNull { finalizer ->
        try {
          finalizer()
          null
        } catch (e: Throwable) {
          e.nonFatalOrThrow()
        }
      }
    Platform.composeErrors(all = errors)?.let { throw it }
  }
}
