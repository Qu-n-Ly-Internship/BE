# Giai đoạn 1: Build (Sử dụng Maven hoặc Gradle để tạo JAR)
# Sử dụng Image JDK đầy đủ để build
FROM maven:3.8.7-jdk-17 as builder 
WORKDIR /app
# Sao chép file cấu hình project (pom.xml hoặc build.gradle)
COPY pom.xml .

# Tải dependencies (tận dụng Docker cache)
RUN mvn dependency:go-offline

# Sao chép mã nguồn và đóng gói
COPY src ./src
RUN mvn package -DskipTests # Build và tạo file JAR

# Giai đoạn 2: Runtime (Image nhỏ hơn, chỉ có JRE)
# Sử dụng JRE (Java Runtime Environment) nhẹ
FROM openjdk:17-jre-slim 
WORKDIR /app

# Sao chép file JAR đã build từ giai đoạn builder
# Tên file JAR thường là 'target/ten-du-an.jar'
COPY --from=builder /app/target/*.jar app.jar 

# Mở cổng mặc định của Spring Boot (thường là 8080)
EXPOSE 8090

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]