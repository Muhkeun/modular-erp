plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    implementation(project(":platform:core"))
    implementation(project(":platform:security"))
    implementation(project(":platform:i18n"))
    implementation(project(":platform:messaging"))
    implementation(project(":platform:web"))

    implementation(project(":modules:master-data"))
    implementation(project(":modules:approval"))
    implementation(project(":modules:document"))

    runtimeOnly(rootProject.libs.postgresql)
    runtimeOnly(rootProject.libs.h2)
    implementation(rootProject.libs.flyway.core)
    implementation(rootProject.libs.flyway.postgres)
}
