# backend/Dockerfile
# Build stage
FROM maven:3.9.9-eclipse-temurin-23 AS build
WORKDIR /app

RUN mkdir -p /app/uploads && chown -R 1000:1000 /app/uploads
# If you use mvnw, copy mvnw and .mvn. Otherwise copy pom and src
COPY pom.xml .
#COPY mvnw .
#COPY .mvn .mvn
# download dependencies (speeds up rebuilds)
RUN mvn -B -ntp -DskipTests dependency:go-offline

COPY src ./src
# build fat jar
RUN mvn -B -ntp -DskipTests package

# Run stage
FROM eclipse-temurin:23-jre
WORKDIR /app

# Create a user for security (optional)
RUN addgroup --system spring && adduser --system --ingroup spring spring

# copy jar from build stage (assumes Spring Boot default jar name)
ARG JAR_FILE
# try default packaged jar if not provided
COPY --from=build /app/target/*.jar app.jar

# create upload dir (for filesystem photo storage)
ENV UPLOAD_DIR=/opt/kyc/uploads
RUN mkdir -p ${UPLOAD_DIR} && chown -R spring:spring ${UPLOAD_DIR}

USER spring
EXPOSE 9100
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -Dspring.config.additional-location=optional:file:/config/application.properties -jar /app/app.jar"]
