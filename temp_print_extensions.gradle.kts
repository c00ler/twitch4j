
tasks.register("printExtensions") {
    doLast {
        project.extensions.asMap.forEach { name, extension ->
            println("Extension name: $name, class: ${extension.javaClass.canonicalName}")
        }
    }
}
