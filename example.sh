#!/usr/bin/env bash
java -jar ./target/scala-2.11/diffy-server.jar \
-candidate='localhost:9200' \
-master.primary='localhost:9000' \
-master.secondary='localhost:9100' \
-service.protocol='http' \
-serviceName='My Service' \
-proxy.port=:8880 \
-admin.port=:8881 \
-http.port=:8888 \
-rootUrl='localhost:8888'
