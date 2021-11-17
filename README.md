# Module Saga

[![Maven Central](https://img.shields.io/maven-central/v/io.github.nomisrev/saga?color=4caf50&label=latest%20release)](https://maven-badges.herokuapp.com/maven-central/io.github.nomisrev/saga)
[![Latest snapshot](https://img.shields.io/badge/dynamic/xml?color=orange&label=latest%20snapshot&prefix=v&query=%2F%2Fmetadata%2Fversioning%2Flatest&url=https%3A%2F%2Fs01.oss.sonatype.org%2Fservice%2Flocal%2Frepositories%2Fsnapshots%2Fcontent%2Fio%2Fgithub%2Fnomisrev%2Fsaga%2Fmaven-metadata.xml)](https://s01.oss.sonatype.org/service/local/repositories/snapshots/content/io/github/nomisrev)
[Website can be found here](https://nomisrev.github.io/Saga)

Add in `build.gradle.kts`

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.nomisrev:saga:0.1.3")
}
```

The saga design pattern is a way to manage data consistency across microservices in distributed transaction scenarios.

A [Saga] is useful when you need to manage data in a consistent manner across services in distributed transaction scenarios.
Or when you need to compose multiple [action]s with a [compensation] that needs to run in a transaction like style.
For example, let's say that we have the following domain types `Order`, `Payment`.

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
//   at io.github.nomisrev.TestKt.awaitSuccess(test.kt:11)
```
