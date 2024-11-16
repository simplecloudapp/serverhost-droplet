plugins {
    `maven-publish`
}

dependencies {
    api(libs.simplecloud.controller)
    api(libs.simplecloud.pubsub)
    api(project(":serverhost-configurator"))
    implementation(rootProject.libs.gson)
    implementation(rootProject.libs.bundles.log4j)
    testImplementation(rootProject.libs.kotlin.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}