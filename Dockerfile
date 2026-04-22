FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/gupet-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]

#ĐỂ DOCKER TỰ BUILD
#FROM maven:3.9.9-eclipse-temurin-21 AS build
#
#WORKDIR /app
#COPY . .
#
#RUN mvn clean package -DskipTests
#
#FROM eclipse-temurin:21-jdk
#
#WORKDIR /app
#COPY --from=build /app/target/*.jar app.jar
#
#EXPOSE 8080
#
#ENTRYPOINT ["java","-jar","app.jar"]