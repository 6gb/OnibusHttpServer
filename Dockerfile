FROM debian:stable-slim
COPY . /usr/src/ohs
WORKDIR /usr/src/ohs
EXPOSE 8080
RUN apt-get update && apt-get install default-jre git && git clone https://github.com/6gb/OnibusHttpServer
CMD ["java", "OnibusHttpServer"]