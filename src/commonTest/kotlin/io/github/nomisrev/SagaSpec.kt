@file:Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")

package io.github.nomisrev

import arrow.fx.coroutines.parSequence
import arrow.fx.coroutines.parTraverse
import arrow.fx.coroutines.parZip
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel

@Suppress("unused")
class SagaSpec :
  StringSpec({
    "Saga returns action result" {
      checkAll(Arb.int()) { i ->
        val saga = saga { i } compensate { fail("Doesn't run") }
        saga.transact() shouldBeExactly i
      }
    }

    class SagaFailed : RuntimeException()

    "Saga runs compensation if throw in builder & rethrows exception" {
      checkAll(Arb.int()) { i ->
        val compensation = CompletableDeferred<Int>()
        val saga = saga {
          saga { i }.compensate { compensation.complete(it) }.bind()
          throw SagaFailed()
        }
        shouldThrow<SagaFailed> { saga.transact() }
        compensation.await() shouldBeExactly i
      }
    }

    "Saga runs compensation if throw in saga & rethrows exception" {
      checkAll(Arb.int()) { i ->
        val compensation = CompletableDeferred<Int>()
        val saga = saga {
          saga { i }.compensate { compensation.complete(it) }.bind()
          saga { throw SagaFailed() }.compensate { fail("Doesn't run") }.bind()
        }
        shouldThrow<SagaFailed> { saga.transact() }
        compensation.await() shouldBeExactly i
      }
    }

    // kotlin.native.concurrent.InvalidMutabilityException: mutation attempt of frozen
    // kotlin.Array@73a59a08
    // https://github.com/Kotlin/kotlinx.coroutines/issues/462
    "Saga runs compensation in order & rethrows exception" {
      checkAll(Arb.int(), Arb.int()) { a, b ->
        val compensations = Channel<Int>(2)
        val saga = saga {
          saga { a }.compensate { compensations.send(it) }.bind()
          saga { b }.compensate { compensations.send(it) }.bind()
          saga { throw SagaFailed() }.compensate { fail("Doesn't run") }.bind()
        }
        shouldThrow<SagaFailed> { saga.transact() }
        compensations.receive() shouldBeExactly b
        compensations.receive() shouldBeExactly a
        compensations.close()
      }
    }

    "Sage composes compensation errors" {
      checkAll(Arb.int()) { a ->
        val compensationA = CompletableDeferred<Int>()
        val original = SagaFailed()
        val compensation = SagaFailed()
        val saga = saga {
          saga { a }.compensate { compensationA.complete(it) }.bind()
          saga {}.compensate { throw compensation }.bind()
          saga { throw original }.compensate { fail("Doesn't run") }.bind()
        }
        val res = shouldThrow<SagaFailed> { saga.transact() }
        res shouldBe original
        res.suppressedExceptions[0] shouldBe compensation
        compensationA.await() shouldBeExactly a
      }
    }

    "Sage composes compensation errors when thrown in block" {
      checkAll(Arb.int()) { a ->
        val compensationA = CompletableDeferred<Int>()
        val original = SagaFailed()
        val compensation = SagaFailed()
        val saga = saga {
          saga { a }.compensate { compensationA.complete(it) }.bind()
          saga {}.compensate { throw compensation }.bind()
          throw original
        }
        val res = shouldThrow<SagaFailed> { saga.transact() }
        res shouldBe original
        res.suppressedExceptions[0] shouldBe compensation
        compensationA.await() shouldBeExactly a
      }
    }

    "Saga can traverse" {
      checkAll(Arb.list(Arb.int())) { iis ->
        iis.mapSaga { saga { it }.compensate { fail("Doesn't run") } }.transact() shouldBe iis
      }
    }

    "Saga can sequence" {
      checkAll(Arb.list(Arb.int())) { iis ->
        iis.map { saga { it }.compensate { fail("Doesn't run") } }.sequence().transact() shouldBe
          iis
      }
    }

    "Saga can parTraverse" {
      checkAll(Arb.list(Arb.int())) { iis ->
        saga { iis.parTraverse { saga { it }.compensate { fail("Doesn't run") }.bind() } }
          .transact() shouldBe iis
      }
    }

    "Saga can parSequence" {
      checkAll(Arb.list(Arb.int())) { iis ->
        saga {
            iis
              .map { suspend { saga { it }.compensate { fail("Doesn't run") }.bind() } }
              .parSequence()
          }
          .transact() shouldBe iis
      }
    }

    "parZip runs left compensation" {
      checkAll(Arb.int()) { a ->
        val compensationA = CompletableDeferred<Int>()
        val latch = CompletableDeferred<Unit>()
        val saga = saga {
          parZip(
            {
              saga {
                  latch.complete(Unit)
                  a
                }
                .compensate { compensationA.complete(it) }
                .bind()
            },
            {
              saga {
                  latch.await()
                  throw SagaFailed()
                }
                .compensate { fail("Doesn't run") }
                .bind()
            }
          ) { _, _ -> }
        }
        shouldThrow<SagaFailed> { saga.transact() }
        compensationA.await() shouldBeExactly a
      }
    }

    "parZip runs right compensation" {
      checkAll(Arb.int()) { a ->
        val compensationB = CompletableDeferred<Int>()
        val latch = CompletableDeferred<Unit>()
        val saga = saga {
          parZip(
            {
              saga {
                  latch.await()
                  throw SagaFailed()
                }
                .compensate { fail("Doesn't run") }
                .bind()
            },
            {
              saga {
                  latch.complete(Unit)
                  a
                }
                .compensate { compensationB.complete(it) }
                .bind()
            }
          ) { _, _ -> }
        }
        shouldThrow<SagaFailed> { saga.transact() }
        compensationB.await() shouldBeExactly a
      }
    }
  })
