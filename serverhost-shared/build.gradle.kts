dependencies {
    api(project(":serverhost-configurator"))
    api(libs.simplecloud.controller)
    api(libs.simplecloud.pubsub)
    api(rootProject.libs.commons.io)

    implementation(rootProject.libs.gson)
    implementation(rootProject.libs.github)
    implementation(rootProject.libs.okhttp)
    implementation(rootProject.libs.bundles.log4j)
    implementation(rootProject.libs.kotlin.reflect)
    implementation(rootProject.libs.ktor.cio)
    implementation(rootProject.libs.commons.compress)
    implementation(rootProject.libs.xz)

    testImplementation(rootProject.libs.kotlin.test)
}