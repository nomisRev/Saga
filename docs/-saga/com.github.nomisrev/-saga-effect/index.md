//[Saga](../../../index.md)/[com.github.nomisrev](../index.md)/[SagaEffect](index.md)

# SagaEffect

[jvm]\
interface [SagaEffect](index.md)

## Functions

| Name | Summary |
|---|---|
| [bind](bind.md) | [jvm]<br>abstract suspend fun <[A](bind.md)> [Saga](../-saga/index.md)<[A](bind.md)>.[bind](bind.md)(): [A](bind.md) |
| [saga](saga.md) | [jvm]<br>open fun <[A](saga.md)> [saga](saga.md)(action: suspend [SagaEffect](index.md).() -> [A](saga.md)): [Saga.Part](../-saga/-part/index.md)<[A](saga.md)> |
