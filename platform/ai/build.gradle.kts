dependencies {
    api(project(":platform:core"))
    implementation(project(":platform:web"))
    implementation(project(":platform:security"))
    implementation(project(":platform:report"))

    implementation(rootProject.libs.spring.boot.starter.web)
    implementation(rootProject.libs.spring.boot.starter.data.jpa)
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    implementation(rootProject.libs.langchain4j.core)
    implementation(rootProject.libs.langchain4j.anthropic)
    implementation(rootProject.libs.langchain4j.embeddings)
    implementation(rootProject.libs.langchain4j.pgvector)

    implementation(rootProject.libs.jackson.module.kotlin)
    implementation(rootProject.libs.springdoc.openapi)

    testImplementation(rootProject.libs.spring.boot.starter.test)
    testImplementation(rootProject.libs.h2)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.springframework.security:spring-security-test")
}
