package io.github.nomisrev

import arrow.core.Either
import arrow.core.computations.EitherEffect
import arrow.core.computations.either

class SagaEitherEffect<E>(
    either: EitherEffect<E, *>,
    saga: SagaEffect
) : EitherEffect<E, Any?> by either as EitherEffect<E, Any?>, SagaEffect by saga

fun <E, A> sagaEither(f: suspend SagaEitherEffect<E>.() -> A): Saga<Either<E, A>> =
    saga {
        either {
            f(SagaEitherEffect(this, this@saga))
        }
    }
