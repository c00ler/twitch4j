package com.github.twitch4j

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity

// This file is mostly ported from AndroidX with minor modifications.
// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/src/main/kotlin/androidx/build/Ktlint.kt

/**
 * Adds a ktlintApplyToIdea task that updates the local .idea files to match the ktlint style.
 */
fun Project.configureKtlintApplyToIdea() {
    if (this.rootProject !== this) {
        throw IllegalArgumentException("Can only use root project for applyToIdea task")
    }
    tasks.register("ktlintApplyToIdea", JavaExec::class.java) {
        description = "Apply ktlint style to idea"
        group = "Tooling"
        classpath = getKtlintConfiguration()
        mainClass.set("com.pinterest.ktlint.Main")
        args = listOf(
            "applyToIDEAProject",
            "-y"
        )
    }
}

/**
 * Configures a "ktlint" task for the project to check formatting and `ktlintFormat` task
 * to fix formatting.
 */
fun Project.configureKtlint() {
    val lintProvider = tasks.register("ktlint", JavaExec::class.java) {
        configureCommonKtlintParams(this@configureKtlint)
        description = "Check Kotlin code style."
        group = "Verification"
        isIgnoreExitValue = true // Don't fail the build if ktlint finds problems - for faster onboarding
        outputs.cacheIf { true }
    }

    afterEvaluate {
        // check task is not available yet, which is why we use afterEvaluate
        project.tasks.named("check").configure {
            dependsOn(lintProvider)
        }
    }

    tasks.register("ktlintFormat", JavaExec::class.java) {
        configureCommonKtlintParams(this@configureKtlint)
        description = "Fix Kotlin code style deviations."
        group = "formatting"
        args = listOf("--format") + this.args
    }
}

/**
 * Configures common ktlint parameters for ktlint tasks
 */
private fun JavaExec.configureCommonKtlintParams(project: Project) {
    val ktlintInputFiles = project.fileTree(project.projectDir).also {
        it.include("**/*.kt")
        it.include("**/*.kts")
        it.exclude("**/testData/**")
        it.exclude("**/build/**")
        it.exclude("**/.*/**")
    }
    val checkstyleOutputFile = project.layout.buildDirectory.file("reports/ktlint/ktlint-checkstyle-report.xml")
    inputs.files(ktlintInputFiles).withPathSensitivity(PathSensitivity.RELATIVE)
    classpath = project.getKtlintConfiguration()
    mainClass.set("com.pinterest.ktlint.Main")
    outputs.file(checkstyleOutputFile)
    workingDir = project.projectDir
    args = listOf(
        "--reporter=plain-summary",
        "--reporter=checkstyle,output=${checkstyleOutputFile.get().asFile.toRelativeString(project.projectDir)}",
    ) + ktlintInputFiles.files.map { it.toRelativeString(project.projectDir) }
}

private fun Project.getKtlintConfiguration(): Configuration =
    configurations.findByName("ktlint") ?: configurations.create("ktlint") {
        val dependency = this@getKtlintConfiguration.dependencies.create("com.pinterest.ktlint:ktlint-cli:1.5.0")
        this.dependencies.add(dependency)
    }
