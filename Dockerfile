FROM hseeberger/scala-sbt

RUN apt-get update

ADD . /usr/local/src
WORKDIR /usr/local/src
RUN ./sbt assembly
RUN mv target/scala-2.11 /opt/diffy

ENTRYPOINT ["java", "-jar", "/opt/diffy/diffy-server.jar"]

CMD [ "-candidate=localhost:9992", \
      "-master.primary=localhost:9990", \
      "-master.secondary=localhost:9991", \
      "-service.protocol=http", \
      "-serviceName='Test-Service'", \
      "-proxy.port=:8880", \
      "-admin.port=:8881", \
      "-http.port=:8888", \
      "-rootUrl=localhost:8888" \
]

