FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package


FROM golang:1.24 AS alpaca-cli-build

RUN CGO_ENABLED=0 go install github.com/alpacahq/cli/cmd/alpaca@latest


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY --from=alpaca-cli-build /go/bin/alpaca /usr/local/bin/alpaca

RUN chmod +x /usr/local/bin/alpaca

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
