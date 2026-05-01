# Build stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/chathub-backend-1.0.0.jar app.jar

# Render dynamic port support
EXPOSE 8000

# Set the server port to 8000 (default for ChatHub)
ENV SERVER_PORT=8000

ENTRYPOINT ["java", "-jar", "app.jar"]
