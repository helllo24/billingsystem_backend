# Step 1: Use Maven to build the project (The Builder)
# We change openjdk to eclipse-temurin for better compatibility
FROM maven:3.8.5-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Use Java to run the project (The Runner)
# We use eclipse-temurin as the reliable replacement for openjdk
FROM eclipse-temurin:17-jre
COPY --from=build /target/*.jar demo.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","demo.jar"]