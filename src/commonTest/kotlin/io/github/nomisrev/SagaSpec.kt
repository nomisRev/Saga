@file:Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")

package io.github.nomisrev

import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel

@Suppress("unused")
class SagaSpec : StringSpec({

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
      try {
        saga.transact()
      } catch (e: SagaFailed) {
        e
      }.shouldNotBeNull()
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
      try {
        saga.transact()
      } catch (e: SagaFailed) {
        e
      }.shouldNotBeNull()
      compensation.await() shouldBeExactly i
    }
  }

  "Saga runs compensation in order & rethrows exception" {
    checkAll(Arb.int(), Arb.int()) { a, b ->
      val compensations = Channel<Int>(2)
      val saga = saga {
        saga { a }.compensate { compensations.send(it) }.bind()
        saga { b }.compensate { compensations.send(it) }.bind()
        saga { throw SagaFailed() }.compensate { throw AssertionError("Doesn't run") }.bind()
      }
      try {
        saga.transact()
      } catch (e: SagaFailed) {
        e
      }.shouldNotBeNull()
      compensations.receive() shouldBeExactly b
      compensations.receive() shouldBeExactly a
      compensations.close()
    }
  }

  "Saga composes compensation errors" {
    checkAll(Arb.int()) { a ->
      val compensationA = CompletableDeferred<Int>()
      val original = SagaFailed()
      val compensation = SagaFailed()
      val saga = saga {
        saga { a }.compensate { compensationA.complete(it) }.bind()
        saga { }.compensate { throw compensation }.bind()
        saga { throw original }.compensate { throw AssertionError("Doesn't run") }.bind()
      }
      val res = try {
        saga.transact()
      } catch (e: SagaFailed) {
        e
      }
      res shouldBe original
      res.suppressedExceptions.firstOrNull() shouldBe compensation
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
        saga { }.compensate { throw compensation }.bind()
        throw original
      }
      val res = try {
        saga.transact()
      } catch (e: SagaFailed) {
        e
      }
      res shouldBe original
      res.suppressedExceptions.firstOrNull() shouldBe compensation
      compensationA.await() shouldBeExactly a
    }
  }

  "Saga can traverse" {
    checkAll(Arb.list(Arb.int())) { ints ->
      ints.traverseSaga { saga { it }.compensate { throw AssertionError("Doesn't run") } }
        .transact() shouldBe ints
    }
  }

  "Saga can sequence" {
    checkAll(Arb.list(Arb.int())) { ints ->
      ints.map { saga { it }.compensate { throw AssertionError("Doesn't run") } }
        .sequence()
        .transact() shouldBe ints
    }
  }

  "Saga can parTraverse" {
    checkAll(Arb.list(Arb.int())) { ints ->
      ints.parTraverseSaga { saga { it }.compensate { throw AssertionError("Doesn't run") } }
        .transact() shouldBe ints
    }
  }

  "Saga can parSequence" {
    checkAll(Arb.list(Arb.int())) { ints ->
      ints.map { saga { it }.compensate { throw AssertionError("Doesn't run") } }
        .parSequence()
        .transact() shouldBe ints
    }
  }

  "parZip runs left compensation" {
    checkAll(Arb.int()) { a ->
      val compensationA = CompletableDeferred<Int>()
      val latch = CompletableDeferred<Unit>()
      val saga = saga {
        latch.complete(Unit)
        a
      }.compensate { compensationA.complete(it) }
        .parZip(saga {
          latch.await()
          throw SagaFailed()
        } compensate { throw AssertionError("Doesn't run") }) { _, _ -> }
      try {
        saga.transact()
      } catch (e: SagaFailed) {
        e
      }.shouldNotBeNull()
      compensationA.await() shouldBeExactly a
    }
  }

  "parZip runs right compensation" {
    checkAll(Arb.int()) { a ->
      val compensationB = CompletableDeferred<Int>()
      val latch = CompletableDeferred<Unit>()
      val saga = saga {
        latch.await()
        throw SagaFailed()
      }.compensate { throw AssertionError("Doesn't run") }
        .parZip(saga {
          latch.complete(Unit)
          a
        } compensate { compensationB.complete(it) }) { _, _ -> }
      try {
        saga.transact()
      } catch (e: SagaFailed) {
        e
      }.shouldNotBeNull()
      compensationB.await() shouldBeExactly a
    }
  }
})