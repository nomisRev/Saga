package io.github.nomisrev

import arrow.core.Option
import arrow.core.computations.OptionEffect
import arrow.core.computations.option

class SagaOptionEffect(
    option: OptionEffect<*>,
    saga: SagaEffect
) : OptionEffect<Any?> by option as OptionEffect<Any?>, SagaEffect by saga

fun <A> sagaOption(f: suspend SagaOptionEffect.() -> A): Saga<Option<A>> =
    saga {
        option<A> {
            f(SagaOptionEffect(this, this@saga))
        }
    }
