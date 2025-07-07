# Utilise une image Java 17
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copie le JAR Spring Boot correctement généré par Maven
COPY target/carts.jar app.jar

# Point d'entrée
ENTRYPOINT ["java", "-jar", "app.jar"]
