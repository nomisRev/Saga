# Saga

[Api Docs](https://nomisrev.github.io/Saga)

The saga design pattern is a way to manage data consistency across microservices in distributed transaction scenarios.

A [Saga] is useful when you need to manage data in a consistent manner across services in distributed transaction scenarios.
Or when you need to compose multiple [action]s with a [compensation] that needs to run in a transaction like style.
For example, let's say that we have following domain types `Order`, `Payment`.

```kotlin
data class Order(val id: UUID, val amount: Long)
data class Payment(val id: UUID, val orderId: UUID)
```

The creation of an `Order` can only remain when a payment has been made.

In SQL you might run this inside a transaction, which can automatically rollback the creation of the `Order` when the creation of the Payment fails.
When you need to do this across distributed services, or a multiple atomic references, etc  you need to manually facilitate the rolling back of the performed actions, or compensating actions.

The [Saga] type, and [saga] DSL remove all the boilerplate of manually having to facilitate this with a convenient suspending DSL.

```kotlin
data class Order(val id: UUID, val amount: Long)

suspend fun createOrder(): Order = Order(UUID.randomUUID(), 100L)
suspend fun deleteOrder(order: Order): Unit = println("Deleting $order")

data class Payment(val id: UUID, val orderId: UUID)

suspend fun createPayment(order: Order): Payment = Payment(UUID.randomUUID(), order.id)
suspend fun deletePayment(payment: Payment): Unit = println("Deleting $payment")

suspend fun Payment.awaitSuccess(): Unit = throw RuntimeException("Payment Failed")

suspend fun main() {
  saga {
    val order = saga { createOrder() }.compensate(::deleteOrder).bind()
    val payment = saga { createPayment(order) }.compensate(::deletePayment).bind()
    payment.awaitSuccess()
  }.transact()
}

// Deleting Payment(id=5753e9bb-248a-4385-8c9c-4a524e80c0f9, orderId=3a55ffab-a3f5-40a9-a2b3-681dc17b174e)
// Deleting Order(id=3a55ffab-a3f5-40a9-a2b3-681dc17b174e, amount=100)
// Exception in thread "main" java.lang.RuntimeException: Payment Failed
//   at com.github.nomisrev.TestKt.awaitSuccess(test.kt:11)
```
