plugins {
    kotlin("jvm") version Jetbrains.Kotlin.version
    id("fabric-loom") version Fabric.Loom.version
    `maven-publish`
}

repositories {
    maven(url = "http://maven.fabricmc.net/") { name = "Fabric" }
    maven(url = "https://server.bbkr.space/artifactory/libs-release") { name = "CottonMC" }
}

minecraft {
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    minecraft("com.mojang", "minecraft", Minecraft.version)
    mappings("net.fabricmc", "yarn", Fabric.YarnMappings.version, classifier = Fabric.YarnMappings.classifier)

    modImplementation("net.fabricmc", "fabric-loader", Fabric.Loader.version)
    modImplementation("net.fabricmc", "fabric-language-kotlin", Fabric.Kotlin.version)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", Fabric.API.version)

    modImplementation(Mods.libgui)
    modImplementation(Mods.modmenu)

    implementation(Google.guava)

    testRuntimeOnly(JUnit.jupiter_engine)

    testImplementation(JUnit.jupiter)
    testImplementation(Google.truth)
}

tasks {
    compileJava {
        targetCompatibility = "1.8"
        sourceCompatibility = "1.8"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=kotlin.ExperimentalStdlibApi"
            )
        }
    }

    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                "modid" to Info.modid,
                "name" to Info.name,
                "version" to Info.version,
                "description" to Info.description,
                "kotlinVersion" to Jetbrains.Kotlin.version,
                "fabricApiVersion" to Fabric.API.version
            )
        }
    }
}
