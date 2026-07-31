import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // NOTE: Gradle 9's plugins {} block cannot resolve buildSrc constants, so
    // the plugin versions are inlined here (kept in sync with Dependencies.kt).
    kotlin("jvm") version "2.4.0"
    id("fabric-loom") version "1.17.13"
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "https://maven.fabricmc.net/") { name = "Fabric" }
    maven(url = "https://staging.alexiil.uk/maven/") { name = "AlexIIL" }
    maven(url = "https://maven.terraformersmc.com/") { name = "TerraformersMC" }
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

    modImplementation(include(Mods.libgui)!!)
    modImplementation(Mods.modmenu)

    testRuntimeOnly(JUnit.jupiter_engine)
    testRuntimeOnly(JUnit.platform_launcher)
    testImplementation(JUnit.jupiter)
    testImplementation(Google.truth)
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "kotlin.ExperimentalStdlibApi"
        )
    }
}

tasks {
    processResources {
        // Uncompressed sound sources live beside the shipped oggs, and are ~56MB of jar otherwise.
        // USAGE.md sits there too, and its uppercase name is an invalid resource path: packing it
        // makes the client log an error for the resource pack on every launch.
        exclude(
            "assets/witness/sounds/raw/**",
            "assets/witness/sounds/pixelated/**",
            "assets/witness/sounds/USAGE.md"
        )

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
