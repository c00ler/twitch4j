// pluginManagement MUST be the first block in the settings file (Gradle requirement).
// Centralise plugin resolution so Gradle does not hit the network on every
// build for plugin metadata. Prefer the Portal and Maven Central; avoids
// individual subprojects declaring their own pluginManagement blocks.
pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
}

// Declare ALL dependency-resolution repositories here (Gradle 7+ best practice).
// Using FAIL_ON_PROJECT_REPOS enforces the rule: no project may silently
// introduce its own repository, which prevents accidental dependency shadowing
// and makes the dependency graph fully reproducible.
//   - mavenCentral: all regular dependencies
//   - google:       metalava tool (com.android.tools.metalava:metalava)
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		google()
	}
}

rootProject.name = "Twitch4J"

include(
	":common",
	":auth",
	":client-websocket",
	":chat",
	":eventsub-common",
	":eventsub-websocket",
	":rest-extensions",
	":rest-helix",
	":rest-kraken",
	":rest-tmi",
	":pubsub",
	":graphql",
	":util",
	":twitch4j",
	":kotlin"
)

project(":common").name = "twitch4j-common"
project(":client-websocket").name = "twitch4j-client-websocket"
project(":auth").name = "twitch4j-auth"
project(":chat").name = "twitch4j-chat"
project(":eventsub-common").name = "twitch4j-eventsub-common"
project(":eventsub-websocket").name = "twitch4j-eventsub-websocket"
project(":rest-extensions").name = "twitch4j-extensions"
project(":rest-helix").name = "twitch4j-helix"
project(":rest-kraken").name = "twitch4j-kraken"
project(":rest-tmi").name = "twitch4j-messaginginterface"
project(":pubsub").name = "twitch4j-pubsub"
project(":graphql").name = "twitch4j-graphql"
project(":util").name = "twitch4j-util"
project(":twitch4j").name = "twitch4j"
project(":kotlin").name = "twitch4j-kotlin"
