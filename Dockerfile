# Builder stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /application

# Copy the built jar from target folder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar

# Extract layers for optimized image (Spring Boot 3+)
RUN java -Djarmode=layertools -jar application.jar extract

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /application

# Copy extracted layers
COPY --from=builder /application/dependencies/ ./dependencies/
COPY --from=builder /application/spring-boot-loader/ ./spring-boot-loader/
COPY --from=builder /application/snapshot-dependencies/ ./snapshot-dependencies/
COPY --from=builder /application/application/ ./application/
COPY --from=builder /application/application.jar ./application.jar

# Expose port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
