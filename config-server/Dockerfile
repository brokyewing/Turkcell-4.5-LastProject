FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY config-server/pom.xml .
COPY config-server/src ./src
COPY configurations ./configurations
RUN mvn clean package -DskipTests -f pom.xml

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Yapılandırma dosyaları: search-locations file:/app/configurations bekliyor.
# docker-compose'da volume mount da var; bu satır mount olmasa da çalışsın diye.
COPY configurations ./configurations
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]

