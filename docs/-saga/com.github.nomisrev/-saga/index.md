//[Saga](../../../index.md)/[com.github.nomisrev](../index.md)/[Saga](index.md)

# Saga

[jvm]\
interface [Saga](index.md)<[A](index.md)>

The saga design pattern is a way to manage data consistency across microservices in distributed transaction scenarios. A [Saga](index.md) is useful when you need to manage data in a consistent manner across services in distributed transaction scenarios. Or when you need to compose multiple actions with a compensation that needs to run in a transaction like style.

For example, let's say that we have following domain types Order, Payment.

data class Order(val id: UUID, val amount: Long)\
data class Payment(val id: UUID, val orderId: UUID)

The creation of an Order can only remain when a payment has been made. In SQL you might run this inside a transaction, which can automatically rollback the creation of the Order when the creation of the Payment fails.

When you need to do this across distributed services, or a multiple atomic references, etc you need to manually facilitate the rolling back of the performed actions, or compensating actions.

The [Saga](index.md) type, and [saga](../saga.md) DSL remove all the boilerplate of manually having to facilitate this with a convenient suspending DSL.

data class Order(val id: UUID, val amount: Long)\
suspend fun createOrder(): Order = Order(UUID.randomUUID(), 100L)\
suspend fun deleteOrder(order: Order): Unit = println("Deleting $order")\
\
data class Payment(val id: UUID, val orderId: UUID)\
suspend fun createPayment(order: Order): Payment = Payment(UUID.randomUUID(), order.id)\
suspend fun deletePayment(payment: Payment): Unit = println("Deleting $payment")\
\
suspend fun Payment.awaitSuccess(): Unit = throw RuntimeException("Payment Failed")\
\
suspend fun main() {\
  saga {\
    val order = saga { createOrder() }.compensate(::deleteOrder).bind()\
    val payment = saga { createPayment(order) }.compensate(::deletePayment).bind()\
    payment.awaitSuccess()\
  }.transact()\
}

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [jvm]<br>class [Builder](-builder/index.md)<[A](-builder/index.md)>(**f**: suspend [SagaEffect](../-saga-effect/index.md).() -> [A](-builder/index.md)) : [Saga](index.md)<[A](-builder/index.md)> |
| [Effect](-effect/index.md) | [jvm]<br>class [Effect](-effect/index.md)<[A](-effect/index.md)>(**f**: suspend [SagaEffect](../-saga-effect/index.md).() -> [A](-effect/index.md)) : [Saga](index.md)<[A](-effect/index.md)> |
| [Full](-full/index.md) | [jvm]<br>class [Full](-full/index.md)<[A](-full/index.md)>(**action**: suspend [SagaEffect](../-saga-effect/index.md).() -> [A](-full/index.md), **compensation**: suspend ([A](-full/index.md)) -> [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) : [Saga](index.md)<[A](-full/index.md)> |
| [Part](-part/index.md) | [jvm]<br>@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.html)()<br>value class [Part](-part/index.md)<[A](-part/index.md)>(**action**: suspend [SagaEffect](../-saga-effect/index.md).() -> [A](-part/index.md)) : [Saga](index.md)<[A](-part/index.md)> |

## Functions

| Name | Summary |
|---|---|
| [compensate](compensate.md) | [jvm]<br>open infix fun [compensate](compensate.md)(compensate: suspend ([A](index.md)) -> [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [Saga](index.md)<[A](index.md)> |
| [parZip](par-zip.md) | [jvm]<br>open fun <[B](par-zip.md), [C](par-zip.md)> [parZip](par-zip.md)(other: [Saga](index.md)<[B](par-zip.md)>, f: suspend CoroutineScope.([A](index.md), [B](par-zip.md)) -> [C](par-zip.md)): [Saga](index.md)<[C](par-zip.md)><br>open fun <[B](par-zip.md), [C](par-zip.md)> [parZip](par-zip.md)(ctx: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), other: [Saga](index.md)<[B](par-zip.md)>, f: suspend CoroutineScope.([A](index.md), [B](par-zip.md)) -> [C](par-zip.md)): [Saga](index.md)<[C](par-zip.md)> |
| [transact](transact.md) | [jvm]<br>open suspend fun [transact](transact.md)(): [A](index.md) |

## Inheritors

| Name |
|---|
| [Saga](-part/index.md) |
| [Saga](-full/index.md) |
| [Saga](-builder/index.md) |
| [Saga](-effect/index.md) |
