object Jetbrains {
    object Kotlin {
        const val version = "1.5.21"
    }
}

object Mods {
    const val modmenu = "com.terraformersmc:modmenu:2.0.4"
    const val libgui = "io.github.cottonmc:LibGui:4.1.6+${Minecraft.version}"
    const val nbtcrafting = "de.siphalor:nbtcrafting-1.17:2+"
}

object Google {
    const val guava = "com.google.guava:guava:30.0-jre"
    const val truth = "com.google.truth:truth:1.0.1"
}

object JUnit {
    const val jupiter_engine = "org.junit.jupiter:junit-jupiter-engine:5.7.0"
    const val jupiter = "org.junit.jupiter:junit-jupiter:5.7.0"
}

/** Check these on https://modmuss50.me/fabric.html */
object Fabric {

    object Kotlin {
        const val version = "1.6.3+kotlin.${Jetbrains.Kotlin.version}"
    }

    object Loader {
        /** https://maven.fabricmc.net/net/fabricmc/fabric-loader/ */
        const val version = "0.11.6"
    }

    object API {
        const val version = "0.37.2+1.17"
    }

    object Loom {
        const val version = "0.8-SNAPSHOT"
    }

    object YarnMappings {
        const val version = "${Minecraft.version}+build.37"
        const val classifier = "v2"
    }
}

object Minecraft {
    const val version = "1.17.1"
}