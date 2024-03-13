import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {

    group = "app.simplecloud.droplet"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            url = URI.create("https://maven.pkg.github.com/thesimplecloud/simplecloud-controller")
            credentials {
                username = (findProperty("gpr.user") ?: System.getenv("USERNAME") ?: "").toString()
                password = (findProperty("gpr.key") ?: System.getenv("TOKEN") ?: "").toString()
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    dependencies {
        testImplementation(rootProject.libs.kotlinTest)
        implementation(rootProject.libs.kotlinJvm)
    }

    kotlin {
        jvmToolchain(17)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
