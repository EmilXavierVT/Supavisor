FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package dependency:copy-dependencies

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./dependency

ENV PORT=3300
EXPOSE 3300

CMD ["java", "-cp", "classes:dependency/*", "app.Main"]
