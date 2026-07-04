object Jetbrains {
    object Kotlin {
        const val version = "2.4.0"
    }
}

object Mods {
    const val modmenu = "com.terraformersmc:modmenu:17.0.0"
    const val libgui = "io.github.cottonmc:LibGui:15.1.0+${Minecraft.version}"
}

object Google {
    const val truth = "com.google.truth:truth:1.4.2"
}

object JUnit {
    const val jupiter_engine = "org.junit.jupiter:junit-jupiter-engine:5.10.2"
    const val jupiter = "org.junit.jupiter:junit-jupiter:5.10.2"
    const val platform_launcher = "org.junit.platform:junit-platform-launcher:1.10.2"
}

/** Check these on https://modmuss50.me/fabric.html */
object Fabric {

    object Kotlin {
        const val version = "1.13.12+kotlin.${Jetbrains.Kotlin.version}"
    }

    object Loader {
        /** https://maven.fabricmc.net/net/fabricmc/fabric-loader/ */
        const val version = "0.19.3"
    }

    object API {
        const val version = "0.141.4+1.21.11"
    }

    object Loom {
        const val version = "1.17.13"
    }

    object YarnMappings {
        const val version = "${Minecraft.version}+build.6"
        const val classifier = "v2"
    }
}

object Minecraft {
    const val version = "1.21.11"
}
