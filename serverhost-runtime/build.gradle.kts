plugins {
    application
}

dependencies {
    api(project(":serverhost-shared"))
    implementation(project(":serverhost-configurator"))
    api(rootProject.libs.kotlinCoroutines)
    implementation(rootProject.libs.bundles.log4j)
    implementation(rootProject.libs.clikt)
    implementation(rootProject.libs.commonsIo)
}

application {
    mainClass = "app.simplecloud.droplet.serverhost.runtime.launcher.LauncherKt"
}