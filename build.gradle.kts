import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // NOTE: Gradle 9's plugins {} block cannot resolve buildSrc constants, so
    // the plugin versions are inlined here (kept in sync with Dependencies.kt).
    kotlin("jvm") version "2.4.10"
    // 26.1+ uses the fully-qualified loom plugin id.
    id("net.fabricmc.fabric-loom") version "1.17.17"
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "https://maven.fabricmc.net/") { name = "Fabric" }
    maven(url = "https://staging.alexiil.uk/maven/") { name = "AlexIIL" }
    maven(url = "https://maven.terraformersmc.com/") { name = "TerraformersMC" }
    maven(url = "https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    minecraft("com.mojang", "minecraft", Minecraft.version)
    // 26.1+ ships unobfuscated: no mappings dependency (Fabric porting guide).

    // 26.1+ uses plain implementation, not modImplementation.
    implementation("net.fabricmc", "fabric-loader", Fabric.Loader.version)
    implementation("net.fabricmc", "fabric-language-kotlin", Fabric.Kotlin.version)
    implementation("net.fabricmc.fabric-api", "fabric-api", Fabric.API.version)

    implementation(include(Mods.libgui)!!)
    implementation(Mods.modmenu)

    // Dev-run only (localRuntime keeps them off the published POM). Modrinth's maven has no
    // dependency metadata, so Sodium must be declared alongside Iris explicitly.
    localRuntime(Mods.sodium)
    localRuntime(Mods.iris)

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
    options.release.set(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
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
