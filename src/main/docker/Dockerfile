FROM code.phoswald.ch/philip/jre-25:latest

ENV TZ                  Europe/Zurich
ENV LANG                de_CH.UTF-8
ENV LC_ALL              de_CH.UTF-8
ENV JAVA_TOOL_OPTIONS   -XX:MaxRAMPercentage=50
ENV APP_SAMPLE_CONFIG   ValueFromDockerfile

WORKDIR /usr/local/application/
COPY maven .
CMD ["java", "-cp", "lib/*", "com.github.phoswald.sample.Application"]

USER daemon:daemon
EXPOSE 8080
