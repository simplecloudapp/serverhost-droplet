import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.sonatype.central.portal.publisher)
    `maven-publish`
}

allprojects {
    group = "app.simplecloud.droplet"
    version = determineVersion()

    repositories {
        mavenCentral()
        maven("https://buf.build/gen/maven")
        maven("https://repo.simplecloud.app/snapshots")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "net.thebugmc.gradle.sonatype-central-portal-publisher")
    apply(plugin = "maven-publish")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.jvm)
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    kotlin {
        jvmToolchain(21)

        compilerOptions {
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks {
        withType<JavaCompile> {
            options.isFork = true
            options.isIncremental = true
        }

        named("shadowJar", ShadowJar::class) {
            mergeServiceFiles()

            archiveFileName.set("${project.name}.jar")
        }

        test {
            useJUnitPlatform()
        }
    }

    centralPortal {
        name = project.name

        username = project.findProperty("sonatypeUsername") as? String
        password = project.findProperty("sonatypePassword") as? String

        pom {
            name.set("SimpleCloud Serverhost Droplet")
            description.set("The server host droplet is one of our first and most important droplets. It is responsible for configuring and managing minecraft servers.")
            url.set("https://github.com/simplecloudapp/serverhost-droplet")

            developers {
                developer {
                    id.set("SimpleCloud Developer")
                }
            }
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                url.set("https://github.com/simplecloudapp/serverhost-droplet.git")
                connection.set("git:git@github.com:theSimpleCloud/simplecloud-controller.git")
            }
        }
    }

    publishing {
        repositories {
            maven {
                name = "simplecloud"
                url = uri(determineRepositoryUrl())
                credentials {
                    username = System.getenv("SIMPLECLOUD_USERNAME")
                        ?: (project.findProperty("simplecloudUsername") as? String)
                    password = System.getenv("SIMPLECLOUD_PASSWORD")
                        ?: (project.findProperty("simplecloudPassword") as? String)
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    signing {
        val releaseType = project.findProperty("releaseType")?.toString() ?: "snapshot"
        if (releaseType != "release") {
            return@signing
        }

        if (hasProperty("signingPassphrase")) {
            val signingKey: String? by project
            val signingPassphrase: String? by project
            useInMemoryPgpKeys(signingKey, signingPassphrase)
        } else {
            useGpgCmd()
        }

        sign(publishing.publications)
    }
}

fun determineVersion(): String {
    val baseVersion = project.findProperty("baseVersion")?.toString() ?: "0.0.0"
    val releaseType = project.findProperty("releaseType")?.toString() ?: "snapshot"
    val commitHash = System.getenv("COMMIT_HASH") ?: "local"

    return when (releaseType) {
        "release" -> baseVersion
        "rc" -> "$baseVersion-rc.$commitHash"
        "snapshot" -> "$baseVersion-SNAPSHOT.$commitHash"
        else -> "$baseVersion-SNAPSHOT.local"
    }
}

fun determineRepositoryUrl(): String {
    val baseUrl = "https://repo.simplecloud.app"
    return when (project.findProperty("releaseType")?.toString() ?: "snapshot") {
        "release" -> "$baseUrl/releases"
        "rc" -> "$baseUrl/rc"
        else -> "$baseUrl/snapshots"
    }
}
