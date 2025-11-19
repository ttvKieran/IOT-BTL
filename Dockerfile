# Build stage: use official Maven + JDK image
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy only pom first to leverage layer caching for dependencies
COPY pom.xml .
# Optional: download dependencies before copying source
RUN mvn -B -ntp dependency:go-offline

# Now copy source (including resources) and build
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage: small JRE image that runs the jar
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar ./app.jar

# Note: Static files are already included in the jar file
# Spring Boot will serve them from classpath:/static/

ENTRYPOINT ["java", "-jar", "/app/app.jar"]