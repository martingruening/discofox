# OpenJDK 11 slim
FROM adoptopenjdk/openjdk11:alpine-jre
# copy fat JAR
WORKDIR /home/discofox
COPY discofox.jar discofox.jar
COPY configuration.json configuration.json
# runs application
CMD ["java", "-jar", "discofox.jar"]
