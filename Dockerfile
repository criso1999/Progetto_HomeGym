# --STAGE 1: build con Maven

# Immagine base 
FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /build
# copy file progetto
COPY pom.xml .
# se hai file settings.xml o .m2, puoi copiarli qui
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests=true

# --- STAGE 2: runtime Tomcat
FROM  tomcat:9.0-jdk17-openjdk
LABEL maintainer="cristianorellana@itstechtalentfactory.it"
# Installazione envsubst (gettex) per templeting semplice
USER root 
RUN apt-get update && apt-get install -y gettext-base && apt-get clean && rm -rf /var/lib/apt/lists/*

# pull la webapp di default (opzionale)
RUN rm -rf /usr/local/tomcat/webapps*

# copiamo il war generato (assumo target/*.war)
COPY --from=builder /build/target/*.war /usr/local/tomcat/webapps/ROOT.war

# Copiamo template e entrypoint
COPY ROOT.xml.template /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml.template
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Esponiamo la porta tomcat
EXPOSE 8080

# start tramite script che genera il file ROOT.xml dal template
ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]