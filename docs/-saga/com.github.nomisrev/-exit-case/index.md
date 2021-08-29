//[Saga](../../../index.md)/[com.github.nomisrev](../index.md)/[ExitCase](index.md)

# ExitCase

[jvm]\
sealed class [ExitCase](index.md)<out [A](index.md)>

## Types

| Name | Summary |
|---|---|
| [Cancelled](-cancelled/index.md) | [jvm]<br>data class [Cancelled](-cancelled/index.md)(**exception**: [CancellationException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines.cancellation/-cancellation-exception/index.html)) : [ExitCase](index.md)<[Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)> |
| [Completed](-completed/index.md) | [jvm]<br>data class [Completed](-completed/index.md)<[A](-completed/index.md)>(**value**: [A](-completed/index.md)) : [ExitCase](index.md)<[A](-completed/index.md)> |
| [Failure](-failure/index.md) | [jvm]<br>data class [Failure](-failure/index.md)(**failure**: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [ExitCase](index.md)<[Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)> |

## Inheritors

| Name |
|---|
| [ExitCase](-completed/index.md) |
| [ExitCase](-cancelled/index.md) |
| [ExitCase](-failure/index.md) |
