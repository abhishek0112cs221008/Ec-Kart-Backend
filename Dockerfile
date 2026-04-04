# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy as build
WORKDIR /app

# Copy the project files
COPY . .

# Set execution permission for mvnw and build the application
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port (Render handles mapping this)
EXPOSE 8080

# Environment variables for Spring Boot
# Note: These are overridden by Render's environment variables
ENV PORT=8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
