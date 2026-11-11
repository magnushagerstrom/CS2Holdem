# Bygger CS2Holdem-servern till en container. Två steg ("multi-stage"):
#   1. build:   en stor bild med Maven och hela JDK:n kompilerar koden till en jar-fil
#   2. runtime: en liten bild med bara Java-runtime (JRE) och jar-filen
# Den färdiga bilden innehåller alltså varken källkod, Maven eller kompilator.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# pom.xml först: beroendena laddas ner i ett eget lager som Docker sparar. Så länge pom.xml
# inte ändras återanvänds lagret, och ett nytt bygge slipper ladda ner allt igen.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Testerna körs i IntelliJ och med ./mvnw test. Integrationstestet behöver Docker, som inte finns här inne.
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# Kör inte som root: om någon hittar ett hål i appen ska de inte äga hela containern
RUN useradd --system --uid 10001 cs2holdem && mkdir -p /app/data && chown cs2holdem /app/data
COPY --from=build /build/target/*.jar app.jar
USER cs2holdem
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
