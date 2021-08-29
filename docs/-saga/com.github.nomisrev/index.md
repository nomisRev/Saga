//[Saga](../../index.md)/[com.github.nomisrev](index.md)

# Package com.github.nomisrev

## Types

| Name | Summary |
|---|---|
| [ExitCase](-exit-case/index.md) | [jvm]<br>sealed class [ExitCase](-exit-case/index.md)<out [A](-exit-case/index.md)> |
| [Saga](-saga/index.md) | [jvm]<br>interface [Saga](-saga/index.md)<[A](-saga/index.md)><br>The saga design pattern is a way to manage data consistency across microservices in distributed transaction scenarios. |
| [SagaEffect](-saga-effect/index.md) | [jvm]<br>interface [SagaEffect](-saga-effect/index.md) |

## Functions

| Name | Summary |
|---|---|
| [guaranteeCase](guarantee-case.md) | [jvm]<br>inline suspend fun <[A](guarantee-case.md)> [guaranteeCase](guarantee-case.md)(fa: suspend () -> [A](guarantee-case.md), crossinline finalizer: suspend ([ExitCase](-exit-case/index.md)<[A](guarantee-case.md)>) -> [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [A](guarantee-case.md) |
| [saga](saga.md) | [jvm]<br>fun <[A](saga.md)> [saga](saga.md)(block: suspend [SagaEffect](-saga-effect/index.md).() -> [A](saga.md)): [Saga](-saga/index.md)<[A](saga.md)> |
