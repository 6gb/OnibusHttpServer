FROM openjdk:8-alpine
COPY . /OnibusHttpServer
WORKDIR /OnibusHttpServer
EXPOSE 8080
RUN apk add git
RUN git clone https://github.com/6gb/OnibusHttpServer
RUN javac OnibusHttpServer.java
CMD ["java", "OnibusHttpServer"]