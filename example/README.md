
# Running the examples

## Localhost testing
The example.sh script included here builds and launches example servers as well as a diffy instance. Verify
that the following ports are available (9000, 9100, 9200, 8880, 8881, & 8888) and run `./example/run.sh start`.

Once your local Diffy instance is deployed, you send it a few requests
like `curl --header "Canonical-Resource: Json" localhost:8880/json?Twitter`. You can then go to your browser at
[http://localhost:8888](http://localhost:8888) to see what the differences across our example instances look like.


## Testing with Docker images

A sample service image is available at the [DockerHub: Diffy repo](https://hub.docker.com/r/diffy/example-service/). The
following should get your production service up an running quickly:

```
>docker pull diffy/example-service:production
>docker run -ti -p 9000:9000 diffy/example-service:production

On a separate shell:
>curl -s -i -H "Canonical-Resource : json" http://localhost:9000/json?Diffy
```

You can pull [diffy's docker image](https://hub.docker.com/r/diffy/diffy/) as well and run it by pointing diffy to whereever you deploy the sample produciton service.

### Notes:
 - Modify the code and run `docker build -t diffy-example-prod -f dockerfiles/ProductionDockerfile .` to build the production image again.
 - Similar command for the candidate image as well.
 - `docker run -ti -p 9000:9000 diffy-example-prod` to run the image
 - On a separate shell run `curl -s -i -H Canonical-Resource : json http://localhost:9000/json?Diffy` to verify the responses
 - When running Diffy's docker image, make sure you have 2 production instances running (primary, secondary) and a single instance of candidate runing as well

