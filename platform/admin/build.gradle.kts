dependencies {
    api(project(":platform:core"))
    api(project(":platform:security"))
    api(project(":platform:preference"))
    api(project(":platform:web"))
    implementation(rootProject.libs.spring.boot.starter.web)
    implementation(rootProject.libs.springdoc.openapi)
}
