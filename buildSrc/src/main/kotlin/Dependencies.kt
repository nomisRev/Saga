object Dependencies {

    object KotlinX {
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
    }

    object Arrow {
        val core = "io.arrow-kt:arrow-core:$arrowVersion"
        val fxCoroutines = "io.arrow-kt:arrow-fx-coroutines:$arrowVersion"
        val fxStm = "io.arrow-kt:arrow-fx-stm:$arrowVersion"
        val optics = "io.arrow-kt:arrow-optics:$arrowVersion"
    }

    object Kotest {
        const val runnerJunit = "io.kotest:kotest-runner-junit5-jvm:$kotestVersion"
        const val assertionsCore = "io.kotest:kotest-assertions-core:$kotestVersion"
        const val property = "io.kotest:kotest-property:$kotestVersion"
    }
}

