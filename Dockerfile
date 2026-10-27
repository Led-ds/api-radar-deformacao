# ==============================================================================
# RADAR++ - BACKEND DOCKERFILE (JAVA 21 / SPRING BOOT 3.3)
# Imagem de produção baseada em JRE 21 leve com execução não-root
# ==============================================================================

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Usuário de serviço sem privilégios administrativos
RUN groupadd -r radar && useradd -r -g radar -s /bin/false radar
USER radar

# Copia pacote compilado
COPY target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
