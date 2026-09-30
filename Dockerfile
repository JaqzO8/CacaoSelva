FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -pl api -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 cacao && useradd --uid 10001 --gid cacao --no-create-home cacao
COPY --from=build --chown=cacao:cacao /src/api/target/api-1.0.0-SNAPSHOT.jar /app/api.jar
USER cacao
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:InitialRAMPercentage=20 -XX:+ExitOnOutOfMemoryError"
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/api.jar"]
