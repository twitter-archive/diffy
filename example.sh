#!/usr/bin/env bash
java -jar ./target/scala-2.11/diffy-server.jar \
-candidate='http-candidate.herokuapp.com:80' \
-master.primary='http-primary.herokuapp.com:80' \
-master.secondary='http-secondary.herokuapp.com:80' \
-service.protocol='http' \
-serviceName='My Service' \
-proxy.port=:8880 \
-admin.port=:8881 \
-http.port=:8888 \
-rootUrl='localhost:8888'
