# --- Stage 1: Dependency Resolution & App Compilation ---
    FROM maven:3.9-eclipse-temurin-17 AS builder
    WORKDIR /app
    
    # Cache dependencies by copying pom.xml first
    COPY pom.xml .
    RUN mvn dependency:go-offline -B
    
    # Copy source tree and compile distribution target
    COPY src ./src
    RUN mvn clean package -DskipTests
    
    # --- Stage 2: Production Execution Environment ---
    FROM eclipse-temurin:17-jre
    WORKDIR /app
    
    # Establish non-root process security privileges
    RUN groupadd -r spring && useradd -r -g spring spring
    USER spring:spring
    
    # Extract deployment artifact from builder container stage
    COPY --from=builder /app/target/*.jar app.jar
    
    EXPOSE 8080
    
    ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
    