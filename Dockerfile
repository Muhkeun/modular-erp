# ---- Backend Build ----
FROM eclipse-temurin:17-jdk-alpine AS backend-build
WORKDIR /app
COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY platform ./platform
COPY modules ./modules
COPY app ./app
RUN chmod +x gradlew && ./gradlew :app:bootJar -x test --no-daemon

# ---- Frontend Build ----
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend ./
RUN npm run build

# ---- Runtime ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Backend
COPY --from=backend-build /app/app/build/libs/*.jar app.jar

# Frontend static files served by nginx (or Spring Boot static)
COPY --from=frontend-build /app/dist ./static

# Expose
EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=postgres

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.web.resources.static-locations=file:./static/"]
