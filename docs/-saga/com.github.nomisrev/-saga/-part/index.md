//[Saga](../../../../index.md)/[com.github.nomisrev](../../index.md)/[Saga](../index.md)/[Part](index.md)

# Part

[jvm]\
@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.html)()

value class [Part](index.md)<[A](index.md)>(**action**: suspend [SagaEffect](../../-saga-effect/index.md).() -> [A](index.md)) : [Saga](../index.md)<[A](index.md)>

## Functions

| Name | Summary |
|---|---|
| [compensate](compensate.md) | [jvm]<br>open infix override fun [compensate](compensate.md)(compensate: suspend ([A](index.md)) -> [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [Saga](../index.md)<[A](index.md)> |
| [parZip](../par-zip.md) | [jvm]<br>open fun <[B](../par-zip.md), [C](../par-zip.md)> [parZip](../par-zip.md)(other: [Saga](../index.md)<[B](../par-zip.md)>, f: suspend CoroutineScope.([A](index.md), [B](../par-zip.md)) -> [C](../par-zip.md)): [Saga](../index.md)<[C](../par-zip.md)><br>open fun <[B](../par-zip.md), [C](../par-zip.md)> [parZip](../par-zip.md)(ctx: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), other: [Saga](../index.md)<[B](../par-zip.md)>, f: suspend CoroutineScope.([A](index.md), [B](../par-zip.md)) -> [C](../par-zip.md)): [Saga](../index.md)<[C](../par-zip.md)> |
| [transact](../transact.md) | [jvm]<br>open suspend fun [transact](../transact.md)(): [A](index.md) |

## Properties

| Name | Summary |
|---|---|
| [action](action.md) | [jvm]<br>val [action](action.md): suspend [SagaEffect](../../-saga-effect/index.md).() -> [A](index.md) |
