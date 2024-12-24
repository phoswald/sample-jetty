FROM eclipse-temurin:23-jre-alpine

ENV TZ                Europe/Zurich
ENV LANG              de_CH.UTF-8
ENV LC_ALL            de_CH.UTF-8
ENV JAVA_TOOL_OPTIONS -XX:MaxRAMPercentage=50
ENV APP_SAMPLE_CONFIG ValueFromDockerfile

USER daemon:daemon
EXPOSE 8080

WORKDIR /usr/local/application/
COPY maven .
CMD ["java", "-cp", "lib/*", "com.github.phoswald.sample.Application"]
