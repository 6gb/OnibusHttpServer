FROM openjdk
COPY . /usr/src/ohs
WORKDIR /usr/src/ohs
EXPOSE 8080
RUN javac OnibusHttpServer.java
CMD ["java", "OnibusHttpServer"]