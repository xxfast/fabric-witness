object Jetbrains {
    object Kotlin {
        // Must match fabric-language-kotlin's bundled Kotlin.
        const val version = "2.4.20"
    }
}

object Mods {
    const val modmenu = "com.terraformersmc:modmenu:21.0.0"
    const val libgui = "io.github.cottonmc:LibGui:18.0.1+26.3-rc-2"

    // Dev-only, for verifying shader compatibility. Iris pins this exact Sodium version.
    const val sodium = "maven.modrinth:sodium:mc${Minecraft.version}-0.9.2-fabric"
    const val iris = "maven.modrinth:iris:1.11.6+${Minecraft.version}-fabric"

    // Dev-only, for loading a reference schematic of the island. Litematica needs this MaLiLib.
    const val litematica = "maven.modrinth:litematica:0.29.0"
    const val malilib = "maven.modrinth:malilib:0.30.1"
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
        const val version = "1.14.1+kotlin.${Jetbrains.Kotlin.version}"
    }

    object Loader {
        /** https://maven.fabricmc.net/net/fabricmc/fabric-loader/ */
        const val version = "0.19.5"
    }

    object API {
        const val version = "0.161.0+26.3"
    }

    object Loom {
        // Example mod for 26.3 uses 1.18-SNAPSHOT; 1.18.x needs Gradle 9.7+.
        const val version = "1.18.2"
    }

    // 26.1+ is unobfuscated: do not declare mappings (see Fabric 26.1 porting guide).
}

object Minecraft {
    const val version = "26.3"
}
