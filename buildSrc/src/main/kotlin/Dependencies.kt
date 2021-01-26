object Jetbrains {
    object Kotlin {
        const val version = "1.4.0"
    }
}

object Mods {
    const val modmenu = "io.github.prospector:modmenu:1.14.6+build.31"
    const val libgui = "io.github.cottonmc:LibGui:3.1.0+${Minecraft.version}"
    const val nbtcrafting = "de.siphalor:nbtcrafting-1.16:2+"
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
        const val version = "${Jetbrains.Kotlin.version}+build.1"
    }

    object Loader {
        /** https://maven.fabricmc.net/net/fabricmc/fabric-loader/ */
        const val version = "0.10.3+build.211"
    }

    object API {
        const val version = "0.24.1+build.412-1.16"
    }

    object Loom {
        const val version = "0.5-SNAPSHOT"
    }

    object YarnMappings {
        const val version = "${Minecraft.version}+build.47"
        const val classifier = "v2"
    }
}

object Minecraft {
    const val version = "1.16.3"
}