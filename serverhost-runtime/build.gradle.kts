
plugins {
    application
}


dependencies {
    api(project(":serverhost-shared"))
    implementation(rootProject.libs.bundles.log4j)
    api("app.simplecloud.controller:controller-shared:1.0.6-SNAPSHOT")
    api("org.spongepowered:configurate-yaml:4.0.0")
    api("org.spongepowered:configurate-extra-kotlin:4.1.2")
    api("commons-io:commons-io:2.15.1")
}

application {
    mainClass = "app.simplecloud.droplet.serverhost.runtime.launcher.LauncherKt"
}