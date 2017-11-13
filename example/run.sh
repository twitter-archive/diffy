#!/usr/bin/env bash

if [ "$1" = "start" ];
then

    # Build primary, secondary, and candidate servers
    javac example/ExampleServers.java && \

    # Deploy primary, secondary, and candidate servers
    java -cp example ExampleServers 9000 9100 9200 & \

    # Build diffy
    ./sbt assembly && \

    # Deploy diffy
    java -jar ./target/scala-2.11/diffy-server.jar \
    -candidate='localhost:9200' \
    -master.primary='localhost:9000' \
    -master.secondary='localhost:9100' \
    -service.protocol='http' \
    -serviceName='My Service' \
    -proxy.port=:8880 \
    -admin.port=:8881 \
    -http.port=:8888 \
    -rootUrl='localhost:8888' &\

else
    echo "Please make sure ports 9000, 9100, 9200, 8880, 8881, & 8888 are available before running \"example/run.sh start\""
fi

