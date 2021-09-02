FROM openjdk:8-alpine
WORKDIR /OnibusHttpServer
ENTRYPOINT /OnibusHttpServer
EXPOSE 8080
RUN apk add git
RUN git clone https://github.com/6gb/OnibusHttpServer
RUN javac OnibusHttpServer/OnibusHttpServer.java
CMD ["java", "OnibusHttpServer/OnibusHttpServer"]