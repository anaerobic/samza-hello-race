FROM ubuntu:14.04

MAINTAINER Michael W. Martin <mwm.cal@gmail.com>

RUN apt-get update && apt-get install -y default-jdk

ENV JAVA_HOME=/usr/lib/jvm/default-java

RUN apt-get install -y git maven curl

COPY app /app

WORKDIR /app

RUN bin/grid install yarn

RUN mvn clean package && rm -r target


