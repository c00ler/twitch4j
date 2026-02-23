package com.github.twitch4j

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.PathSensitivity

fun Project.configureListDependencies() {
    tasks.register("listDependencies") {
        doLast {
            val configurationDependencies = this@configureListDependencies
                .getConfigurationsDependencies()
                .getDependenciesPathsMap()
            configurationDependencies.forEach { (configuration, dependencies) ->
                if (dependencies.isNotEmpty()) {
                    logger.lifecycle("$project:$configuration -> Dependencies:$dependencies")
                }
            }
        }
    }
}

fun Project.configureDependencyReport() {
    val dependencyReport = tasks.register("dependencyReport") {
        group = "documentation"
        description = "Creates a dependency report and saves it in projects build/reports/dependencies directory."
        val report = this@configureDependencyReport
            .layout.buildDirectory.file("reports/dependencies/dependencyReport.md")
        val buildGradleFiles = rootProject.fileTree(rootDir).also {
            it.include("**/*.gradle")
            it.include("**/*.gradle.kts")
        }.files.map { it.toRelativeString(projectDir) }
        inputs.property("project", project.toString())
        inputs.files(buildGradleFiles).withPropertyName("buildGradleFiles")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.files(report).withPropertyName("dependencyReport")
        outputs.cacheIf { true }

        doLast {
            val configurationDependencies = this@configureDependencyReport
                .getConfigurationsDependencies()
                .getDependenciesPathsMap()

            val dependenciesReport = configurationDependencies.map { (configuration, dependencies) ->
                "## $configuration -> Dependencies:$dependencies"
            }.fold("# Dependencies for $project") { acc, configurationReport ->
                acc + "\n\n" + configurationReport
            }
            report.get().asFile.writeText(dependenciesReport)
        }
    }

    val dependencyReportRoot = tasks.register("dependencyReportRoot", Copy::class.java) {
        group = "documentation"
        description =
            "Collects all the subprojects dependency reports in the root project build/reports/dependencies directory."
        from(dependencyReport.get().outputs.files.asPath)
        into(rootProject.layout.buildDirectory.dir("reports/dependencies"))
        val proj = project.name.replace(":", "_").replace(" ", "-").replace("'", "")
        rename("(.*).md", "$1_$proj.md")
        dependsOn(dependencyReport)
    }

    afterEvaluate {
        // check task is not available yet, which is why we use afterEvaluate
        project.tasks.named("check").configure {
            dependsOn(dependencyReport)
            dependsOn(dependencyReportRoot)
        }
    }
}

private fun Project.getConfigurationsDependencies(): Map<String, Set<ResolvedDependency>> {
    return configurations.filter { it.isCanBeResolved }.associate { configuration ->
        configuration.name to configuration.resolvedConfiguration.firstLevelModuleDependencies
    }
}

private fun Map<String, Set<ResolvedDependency>>.getDependenciesPathsMap(): Map<String, String> {
    return this.mapValues {
        it.value.sortedBy { it.name }.fold("") { acc, dep ->
            "$acc\n\t${dep.name}"
        }
    }
}
