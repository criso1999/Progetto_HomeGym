# ---------- STAGE 1: build with OpenJDK + apt-installed Maven ----------
FROM eclipse-temurin:17-jdk AS builder
LABEL stage=builder
WORKDIR /build

# install maven (Debian-based)
RUN apt-get update \
 && apt-get install -y maven --no-install-recommends \
 && rm -rf /var/lib/apt/lists/*

# copy and resolve dependencies offline
COPY pom.xml .
RUN mvn -B dependency:go-offline

# copy sources and build
COPY src ./src
RUN mvn -B package -DskipTests=true

# ---------- STAGE 2: runtime Tomcat ----------
FROM tomcat:9.0-jdk17-openjdk
LABEL maintainer="cristianorellana@itstechtalentfactory.it"

USER root
# install envsubst (gettext) for simple templating
RUN apt-get update \
 && apt-get install -y gettext-base --no-install-recommends \
 && rm -rf /var/lib/apt/lists/*

# clean default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# copy war from builder
COPY --from=builder /build/target/*.war /usr/local/tomcat/webapps/ROOT.war

# copy templates + entrypoint
COPY ROOT.xml.template /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml.template
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh


EXPOSE 8080


ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]
