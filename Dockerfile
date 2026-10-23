# syntax=docker/dockerfile:1

# ---------- 构建阶段：Maven + JDK 17 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先复制 pom，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- 运行阶段：JDK 17 + Tomcat 容器 ----------
FROM tomcat:10.1-jdk17-temurin
WORKDIR /usr/local/tomcat

ENV TZ=Asia/Shanghai \
    CATALINA_OPTS="-Xms256m -Xmx512m"

# 清掉 Tomcat 自带示例应用，把 WAR 作为 ROOT 应用部署
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /build/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
