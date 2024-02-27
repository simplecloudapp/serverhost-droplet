import java.net.URI

plugins {
    application
}

repositories {
    maven {
        url = URI.create("https://maven.pkg.github.com/thesimplecloud/simplecloud-controller")
        credentials {
            username = (findProperty("gpr.user") ?: System.getenv("USERNAME") ?: "").toString()
            password = (findProperty("gpr.key") ?: System.getenv("TOKEN") ?: "").toString()
        }
    }
}

dependencies {
    api(project(":serverhost-shared"))
    implementation(rootProject.libs.bundles.log4j)
    implementation("app.simplecloud.controller:controller-shared:1.0-SNAPSHOT")
}

application {
    mainClass = "app.simplecloud.droplet.serverhost.runtime.LauncherKt"
}