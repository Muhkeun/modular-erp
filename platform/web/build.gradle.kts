dependencies {
    api(project(":platform:core"))
    api(rootProject.libs.spring.boot.starter.web)
    api(rootProject.libs.springdoc.openapi)
    compileOnly(rootProject.libs.spring.boot.starter.security)
}
