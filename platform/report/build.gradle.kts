dependencies {
    api(project(":platform:core"))
    implementation(project(":platform:web"))
    implementation(rootProject.libs.spring.boot.starter.web)
    implementation(rootProject.libs.spring.boot.starter.data.jpa)
    implementation(rootProject.libs.itext.core)
    implementation(rootProject.libs.itext.html2pdf)
    implementation(rootProject.libs.poi.ooxml)
    implementation(rootProject.libs.springdoc.openapi)

    testImplementation(rootProject.libs.spring.boot.starter.test)
    testImplementation(rootProject.libs.h2)
}
