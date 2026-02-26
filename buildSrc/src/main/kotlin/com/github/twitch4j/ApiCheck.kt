package com.github.twitch4j

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

val API_BASE_FILE = "api.base"

/**
 * Metalava is a metadata generator intended for JVM type projects.
 * The main users of this tool are Android Platform and AndroidX libraries, however this tool also works on non-Android libraries.
 *
 * Allows extracting the API (into signature text files, into stub API files which in turn get compiled into android.jar,
 * the Android SDK library) and more importantly to hide code intended to be implementation only,
 * driven by javadoc comments like @hide, @doconly, @removed, etc, as well as various annotations.
 *
 * Extracting source level annotations into external annotations file (such as the typedef annotations,
 * which cannot be stored in the SDK as .class level annotations) to ship alongside the Android SDK and used by Android Lint.
 *
 * Diffing versions of the API and determining whether a newer version is compatible with the older version. (See COMPATIBILITY.md)
 *
 * More info available at https://android.googlesource.com/platform/tools/metalava/?pli=1#
 */
fun Project.configureMetalava() {
    // NOTE: repository declaration removed – google() is now declared centrally
    // in settings.gradle.kts via dependencyResolutionManagement so that all
    // projects share a single, authoritative list of dependency repositories.

    /**
     * The checkApi task checks the compatibility of the current API with the API base file.
     * It prints the compatibility report to the console.
     * If the API is not compatible, the task fails.
     */
    val checkProvider = tasks.register("checkApi", JavaExec::class.java) {
        configureCommonMetalavaArgs(this@configureMetalava)
        description = "Check API compatibility."
        group = "Verification"
        args = listOf("--check-compatibility:api:released", API_BASE_FILE) + args
        inputs.files(API_BASE_FILE).withPropertyName("apiCheckBaseFile").withPathSensitivity(PathSensitivity.RELATIVE)
    }

    afterEvaluate {
        // check task is not available yet, which is why we use afterEvaluate
        project.tasks.named("check").configure {
            dependsOn(checkProvider)
        }
        checkProvider.configure {
            dependsOn(project.tasks.named("assemble"))
        }
    }

    /**
     * The updateApi task updates the API base file with the current API.
     * It prints the compatibility report to the console.
     */
    tasks.register("updateApi", JavaExec::class.java) {
        configureCommonMetalavaArgs(this@configureMetalava)
        description = "Update API base file."
        group = "formatting"
        args = listOf("--api", API_BASE_FILE) + args
    }
}

/**
 * Configures common Metalava parameters.
 *
 * Previously the compile classpath was resolved eagerly at configuration time
 * via `configuration.files`, which forced dependency resolution before any task
 * had been executed and prevented Gradle's incremental-build machinery from
 * tracking it as a proper task input.
 *
 * The fix uses [CommandLineArgumentProvider] so that both the classpath and the
 * source-file list are computed lazily at task-execution time.  The classpath
 * configuration is also registered as a formal task input (via
 * `inputs.files(configurations.named(...))`) so that Gradle can correctly detect
 * when it has changed and must invalidate the UP-TO-DATE / build-cache result.
 */
private fun JavaExec.configureCommonMetalavaArgs(project: Project) {
    val jdkHome = org.gradle.internal.jvm.Jvm.current().javaHome.absolutePath
    val apiFiles = project.fileTree(project.projectDir).also {
        it.include("**/*.kt")
        it.include("**/*.java")
        it.exclude("**/testData/**")
        it.exclude("**/build/**")
        it.exclude("**/.*/**")
    }
    // Register the file tree lazily (not .files which resolves eagerly).
    inputs.files(apiFiles).withPropertyName("apiCheckInputFiles").withPathSensitivity(PathSensitivity.RELATIVE)
    // Declare compileClasspath as a proper task input so Gradle can track it for
    // UP-TO-DATE checks and build-cache invalidation.
    inputs.files(project.configurations.named("compileClasspath"))
        .withPropertyName("compileClasspath")
        .withPathSensitivity(PathSensitivity.NONE)
    classpath = project.getMetalavaConfiguration()
    mainClass.set("com.android.tools.metalava.Driver")
    // Use an argumentProvider so the classpath files are resolved at task-execution
    // time rather than at configuration time, keeping configuration fast and
    // compatible with the Gradle build cache.
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "--jdk-home", jdkHome,
            "--classpath", project.configurations.getByName("compileClasspath").files
                .joinToString(":") { it.toRelativeString(project.projectDir) },
            "--source-files",
        ) + apiFiles.files.map { it.toRelativeString(project.projectDir) }
    })
}

private fun Project.getMetalavaConfiguration(): Configuration {
    return configurations.findByName("metalava") ?: configurations.create("metalava") {
        val dependency =
            this@getMetalavaConfiguration.dependencies.create("com.android.tools.metalava:metalava:1.0.0-alpha12")
        dependencies.add(dependency)
    }
}
