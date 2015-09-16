FROM williamyeh/scala:2.11.6

RUN apt-get update && apt-get install -y curl

ADD . /tmp
WORKDIR /tmp
RUN ./sbt assembly

