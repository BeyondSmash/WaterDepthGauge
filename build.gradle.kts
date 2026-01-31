plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.underwaterdepth"
version = "1.0.3"

repositories {
    mavenCentral()
    maven { url = uri("https://www.cursemaven.com") }
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    compileOnly(files("libs/MultipleHUD-1.0.4.jar"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    compileJava {
        options.release.set(25)
    }

    shadowJar {
        archiveBaseName.set("WaterDepthGauge")
        archiveClassifier.set("")
        archiveVersion.set("1.0.3")
    }

    build {
        dependsOn(shadowJar)
    }
}
