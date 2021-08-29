//[Saga](../../index.md)/[com.github.nomisrev](index.md)/[guaranteeCase](guarantee-case.md)

# guaranteeCase

[jvm]\
inline suspend fun <[A](guarantee-case.md)> [guaranteeCase](guarantee-case.md)(fa: suspend () -> [A](guarantee-case.md), crossinline finalizer: suspend ([ExitCase](-exit-case/index.md)<[A](guarantee-case.md)>) -> [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [A](guarantee-case.md)
