# ================== Build Stage ==================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first for better dependency caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn -B dependency:go-offline -DskipTests

# Copy source code
COPY src ./src

# Build the application (force update + clean build)
RUN mvn -B clean package -DskipTests -U --no-transfer-progress

# ================== Runtime Stage ==================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/service-connect-hub-1.0.0.jar app.jar

EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]