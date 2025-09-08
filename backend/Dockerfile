# 使用官方 Eclipse Temurin JDK 17 镜像
FROM eclipse-temurin:17-jdk-jammy

# 设置工作目录
WORKDIR /app

# 复制后端 jar 文件
COPY target/box-0.0.1-SNAPSHOT.jar app.jar

# 暴露容器端口
EXPOSE 8080

# 启动 Spring Boot 后端
ENTRYPOINT ["java", "-jar", "app.jar"]