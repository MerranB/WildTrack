 FROM maven:3.9-eclipse-temurin-25 AS build
  WORKDIR /app

  COPY pom.xml ./
  RUN mvn dependency:go-offline -q

  COPY src/ src/
  RUN mvn package -DskipTests -q

  FROM eclipse-temurin:25-jre
  WORKDIR /app

  RUN groupadd -r spring && useradd -r -g spring spring

  COPY --from=build /app/target/wildtrack-*.jar app.jar

  USER spring

  EXPOSE 8080

  ENTRYPOINT ["java", "-jar", "app.jar"]