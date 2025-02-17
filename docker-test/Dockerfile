FROM openjdk:21-slim

EXPOSE 25565
COPY data/serverhost-configurator.jar /configurator.jar
COPY data/server.jar /server.jar
COPY data/configurator.yml /configurator.yml

ENV SIMPLECLOUD_COMMAND="java -jar -Dcom.mojang.eula.agree=true server.jar nogui"
ENV FORWARDING_SECRET="1234"
ENV SIMPLECLOUD_PREFIX=""
ENV MAX_PLAYERS=130


CMD ["java", "-jar", "configurator.jar"]