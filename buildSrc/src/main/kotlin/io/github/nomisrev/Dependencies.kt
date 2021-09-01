package io.github.nomisrev

object Dependencies {

    object KotlinX {
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    }

    object Arrow {
        val core = "io.arrow-kt:arrow-core:${Versions.arrow}"
        val fxCoroutines = "io.arrow-kt:arrow-fx-coroutines:${Versions.arrow}"
        val fxStm = "io.arrow-kt:arrow-fx-stm:${Versions.arrow}"
        val optics = "io.arrow-kt:arrow-optics:${Versions.arrow}"
    }

    object Kotest {
        const val runnerJunit = "io.kotest:kotest-runner-junit5-jvm:${Versions.kotest}"
        const val assertionsCore = "io.kotest:kotest-assertions-core:${Versions.kotest}"
        const val property = "io.kotest:kotest-property:${Versions.kotest}"
    }
}

