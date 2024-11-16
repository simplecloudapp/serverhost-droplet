plugins {
    application
}

dependencies {
    api(project(":serverhost-shared"))
    api(rootProject.libs.kotlin.coroutines)
    implementation(rootProject.libs.simplecloud.metrics)
    implementation(rootProject.libs.bundles.log4j)
    implementation(rootProject.libs.clikt)
    implementation(rootProject.libs.commons.io)
}

application {
    mainClass = "app.simplecloud.droplet.serverhost.runtime.launcher.LauncherKt"
}