package com.example.ksppluginsetup.ksp.feature.greeting

import com.example.ksppluginsetup.ksp.feature.greeting.scenario.excludeScenarios
import com.example.ksppluginsetup.ksp.feature.greeting.scenario.propertyShapeScenarios
import com.example.ksppluginsetup.ksp.testing.compile.runCompileSnapshotTest
import com.example.ksppluginsetup.ksp.testing.generator.Generator
import com.example.ksppluginsetup.ksp.testing.generator.cartesian
import com.example.ksppluginsetup.ksp.testing.generator.union
import com.example.ksppluginsetup.ksp.testing.generator.validExampleOptions
import io.kotest.core.spec.style.FreeSpec

/**
 * Generator-driven golden coverage for `@Greeting` — the workhorse of the suite.
 *
 * Scenario families are unioned, then crossed with the option axis, and every resulting point
 * becomes one test case and one golden file. Adding a case to a `scenario/` file therefore extends
 * coverage across every representative option set at once.
 *
 * Record what is deliberately NOT covered here, and why. A reader should not have to guess whether
 * an absent case is an oversight or a decision.
 */
internal class GreetingSnapshotTest :
    FreeSpec({
        "All patterns" - {
            cartesian(
                union {
                    "propertyShape" case propertyShapeScenarios()
                    "exclude" case excludeScenarios()
                },
                Generator.validExampleOptions(),
                label = { scenario, options -> "option=$options/$scenario" },
            ).representativeValues()
                .forEach { (testCaseName, value) ->
                    val (scenario, options) = value

                    // Test names become golden file names: never use ':' (invalid on Windows) and
                    // never embed a sequence number (renumbering would rewrite every golden).
                    testCaseName!! {
                        runCompileSnapshotTest(inputs = scenario.files, options = options)
                    }
                }
        }
    })
