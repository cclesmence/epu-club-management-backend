FROM maven:3.9.9-amazoncorretto-21-alpine AS build

WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src

RUN mvn clean package -DskipTests=true


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

CMD ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]