plugins {
    `maven-publish`
}

dependencies {
    api(libs.simpleCloudController)
    api(libs.simpleCloudPubSub)
    implementation(project(":serverhost-configurator"))
    implementation(rootProject.libs.gson)
    implementation(rootProject.libs.bundles.log4j)
    testImplementation(rootProject.libs.kotlinTest)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}