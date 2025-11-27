# Sử dụng JRE (Java Runtime Environment) nhẹ
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Sao chép file JAR đã build từ giai đoạn builder
COPY /target/be-0.0.1-SNAPSHOT.jar ./be.jar

# Mở cổng mặc định của Spring Boot (thường là 8080)
EXPOSE 8090

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "be.jar"]