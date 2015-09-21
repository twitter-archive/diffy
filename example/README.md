This is an example of diffy. 

## Running

To run it you will need docker and docker-compose installed. Read more about it here: https://www.docker.com/toolbox

In this directory, run docker compose up command:

      $ docker-compose up

It will download the needed images and start the application. You will notice that the logs for all the four services: the twitter diffy proxy, the candidate service, the primary and secondary services. 
You will be able to access the web console for diffy on http://localhost:8888/

Now you can curl to check the differences:

      $ curl http://localhost:31900/endpoint

In the web console you will be able to see the diffs. 

To change the services the only thing you need to do is to change the three flavor files withing the flavors directory, candidate, primary and secondary. Whatever you change there will change when you restart the docker-compose.

ps: If you are running on mac or windows, remember to use the docker-machine/boot2docker/whereveryourundockeron ip instead of localhost, or do port forward it to localhost.

Have fun!

## Digging a bit more

There are some other resources running in the same containers. You can play with it with the following curls:

      $ curl --header "Canonical-Resource: /endpoint" http://localhost:31900/endpoint
      $ curl --header "Canonical-Resource: /endpoint/foo" http://localhost:31900/endpoint/foo
      $ curl --header "Canonical-Resource: /endpoint/meh" http://localhst:31900/endpoint/meh

## Applying to your own service

As you can see in the docker-compose.yml, you can replace the container with two different versions of your service and expose the ports in a way to keep the same configuratio in both services. For example:

      candidate:
        image: mycompany/my_service:new_version
        ports:
          - "8080"
      
      primary:
        image: mycompany/my_service:stable_version
        ports:
          - "8080"
      
      secondary:
        image: mycompany/my_service:stable_version
        ports:
          - "8080"

Than run docker-compose up again.

## What is the service being tested here?

It is a rest-shifter service. Easy way to create simple service mocks and prototypes. 
Read more: https://github.com/camiloribeiro/restshifter

This example was originaly posted on https://github.com/camiloribeiro/dockdiffy under the Apache License, Version 2.0 (the "License").
