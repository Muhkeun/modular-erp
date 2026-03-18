dependencies {
    api(project(":platform:core"))
    api(rootProject.libs.spring.boot.starter.security)
    api(rootProject.libs.spring.boot.starter.web)
    implementation(rootProject.libs.jjwt.api)
    runtimeOnly(rootProject.libs.jjwt.impl)
    runtimeOnly(rootProject.libs.jjwt.jackson)
}
