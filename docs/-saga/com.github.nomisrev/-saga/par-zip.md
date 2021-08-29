//[Saga](../../../index.md)/[com.github.nomisrev](../index.md)/[Saga](index.md)/[parZip](par-zip.md)

# parZip

[jvm]\
open fun <[B](par-zip.md), [C](par-zip.md)> [parZip](par-zip.md)(other: [Saga](index.md)<[B](par-zip.md)>, f: suspend CoroutineScope.([A](index.md), [B](par-zip.md)) -> [C](par-zip.md)): [Saga](index.md)<[C](par-zip.md)>

open fun <[B](par-zip.md), [C](par-zip.md)> [parZip](par-zip.md)(ctx: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), other: [Saga](index.md)<[B](par-zip.md)>, f: suspend CoroutineScope.([A](index.md), [B](par-zip.md)) -> [C](par-zip.md)): [Saga](index.md)<[C](par-zip.md)>
