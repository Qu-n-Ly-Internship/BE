# Sử dụng JDK 17 nhẹ
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy file jar
COPY target/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
