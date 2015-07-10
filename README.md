# samza-hello-race
An example of using Samza to process the generate-race read stream!

Startup Kafka with:
```sh
docker run -d -p 2181:2181 -h zookeeper.lacolhost.com --name zookeeper confluent/zookeeper

docker run -d -p 9092:9092 -h kafka.lacolhost.com --name kafka --link zookeeper:zookeeper confluent/kafka
```

Get up and running in no time in Docker with: ```docker run --rm --net host -it -p 8088:8088 anaerobic/samza-hello-race bash```

Use these commands to start YARN:
```sh
cd hello-race/

bin/grid start yarn
```

Navigate to http://localhost:8088/cluster/apps and you should see the Hadoop elephant in the top-left.

Use these commands to package and "deploy" the samza job:
```sh
mvn clean package

mkdir -p deploy/samza

tar -xvf ./target/hello-race-0.9.0-dist.tar.gz -C deploy/samza
```

Use these commands to start up some Kafka consumers to watch the magic happen in real-time:
```sh
docker run -it --rm --net host --name consume-reads anaerobic/fsharp-kafka-consumer reads http://kafka.lacolhost.com:9092

docker run -it --rm --net host --name consume-bib-aggregates anaerobic/fsharp-kafka-consumer race-bib-aggregates http://kafka.lacolhost.com:9092

docker run -it --rm --net host --name consume-bib-checkpoints anaerobic/fsharp-kafka-consumer race-checkpoint-checks http://kafka.lacolhost.com:9092
```

Use these commands to run the jobs:
```sh
deploy/samza/bin/run-job.sh --config-factory=org.apache.samza.config.factories.PropertiesConfigFactory --config-path=file://$PWD/deploy/samza/config/race-bib-aggregator.properties

deploy/samza/bin/run-job.sh --config-factory=org.apache.samza.config.factories.PropertiesConfigFactory --config-path=file://$PWD/deploy/samza/config/race-checkpoint-checker.properties
```

Use this command to kick off the race and pipe the results into a Kafka producer:
```sh
docker run --rm -i anaerobic/generate-race 5000 3 | docker run -i --rm --net host anaerobic/fsharp-kafka-producer reads http://kafka.lacolhost.com:9092
```

Use this command to kill one of the jobs: ```deploy/samza/bin/kill-yarn-job.sh <appID>```

Use this command to kill YARN: ```bin/grid stop yarn```
