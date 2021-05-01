plugins {
    kotlin("jvm") version Jetbrains.Kotlin.version
    id("fabric-loom") version Fabric.Loom.version
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "http://maven.fabricmc.net/") { name = "Fabric" }
    maven(url = "https://server.bbkr.space/artifactory/libs-release") { name = "CottonMC" }
    maven(url = "https://maven.siphalor.de") { name = "Siphalor's Maven" }
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
    modImplementation(Mods.nbtcrafting)

    implementation(Google.guava)

    testRuntimeOnly(JUnit.jupiter_engine)

    testImplementation(JUnit.jupiter)
    testImplementation(Google.truth)
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
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

    jar {
        from("LICENSE")
    }
}

