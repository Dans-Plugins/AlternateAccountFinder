FROM ubuntu:24.04

# Install dependencies
# 1.19.4 is the supported version (see minecraft-versions.json) that still runs on Java 17.
# The newer supported versions need a newer Java, and the Gradle wrapper (8.1.1) cannot run on it.
RUN apt-get update
RUN apt-get install -y git \
    openjdk-17-jdk \
    openjdk-17-jre \
    wget

# Create server directory
WORKDIR /testmcserver

# Build server
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
RUN git config --global --unset core.autocrlf || :
RUN java -jar BuildTools.jar --rev 1.19.4
RUN echo "eula=true" > eula.txt
RUN mkdir plugins

# Build plugin
COPY . .
RUN ./gradlew build
RUN cp build/libs/*-all.jar plugins

# Run server
EXPOSE 25565
ENTRYPOINT java -jar spigot-1.19.4.jar
