object Jetbrains {
    object Kotlin {
        // Must match fabric-language-kotlin's bundled Kotlin.
        const val version = "2.4.10"
    }
}

object Mods {
    const val modmenu = "com.terraformersmc:modmenu:20.0.1"
    const val libgui = "io.github.cottonmc:LibGui:17.0.0+${Minecraft.version}"
}

object Google {
    const val truth = "com.google.truth:truth:1.4.2"
}

object JUnit {
    const val jupiter_engine = "org.junit.jupiter:junit-jupiter-engine:5.10.2"
    const val jupiter = "org.junit.jupiter:junit-jupiter:5.10.2"
    const val platform_launcher = "org.junit.platform:junit-platform-launcher:1.10.2"
}

/** Check these on https://fabricmc.net/develop */
object Fabric {

    object Kotlin {
        const val version = "1.13.13+kotlin.${Jetbrains.Kotlin.version}"
    }

    object Loader {
        /** https://maven.fabricmc.net/net/fabricmc/fabric-loader/ */
        const val version = "0.19.3"
    }

    object API {
        const val version = "0.156.0+26.2"
    }

    object Loom {
        // Example mod for 26.2 uses 1.17-SNAPSHOT; pin a stable 1.17.x when available.
        const val version = "1.17.17"
    }

    // 26.1+ is unobfuscated: do not declare mappings (see Fabric 26.1 porting guide).
}

object Minecraft {
    const val version = "26.2"
}
