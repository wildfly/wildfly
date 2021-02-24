# Tweaking the Embedded Kafka Broker

The embedded Kafka broker does not work out of the box with the default properties.
Determining the right properties was a process of 'informed' trial and error. I am documenting
the steps here in case something changes in future releases of Kafka or the spring-kafka-test
library used to boot embedded Kafka.

Essentially I disabled the RunKafkaSetupTask and tried running the Kafka tests against Kafka from the
following **working** setups:

* Running Kafka as mentioned in the Quickstart [http://kafka.apache.org/quickstart]
* Running Kafka in Docker using a `docker-compose.yml` file as listed in the resources section below

Both of these print out a list of the properties used by the broker in their respective consoles.

Next I grabbed the properties used by the embedded broker. To do this, I needed to add the following
dependency to the pom:
```
   <dependency>
       <groupId>org.jboss.logmanager</groupId>
       <artifactId>log4j-jboss-logmanager</artifactId>
       <scope>test</scope>
   </dependency>
```
I then created a `src/test/resources/logging.properties` as shown in the resources section below.

I created a 'throwaway' main class containing the code from RunKafkaSetupTask to start the embedded server.
I then run that class to start Kafka. I was doing this from my IDE, and the logs show up in
`<wildfly-root-checkout-folder>/target/test/.log`.

Once I have all the logs, I looked for entries under 'KafkaConfig values' which looked very different in the
embedded case versus the two above working cases. I then tweak the EmbeddedKafkaBroker properties and try again.
Originally I had very many properties, but believe I now have trimmed it down to the essential.


## Resources
### docker-compose.yml
To run Kafka in Docker need to create a `docker-compose.yml` file with the following contents:
```
version: '2'
services:
  zookeeper:
    image: strimzi/kafka:0.11.3-kafka-2.1.0
    command: [
      "sh", "-c",
      "bin/zookeeper-server-start.sh config/zookeeper.properties"
    ]
    ports:
      - "2181:2181"
    environment:
      LOG_DIR: /tmp/logs

  kafka:
    image: strimzi/kafka:0.11.3-kafka-2.1.0
    command: [
      "sh", "-c",
      "bin/kafka-server-start.sh config/server.properties --override listeners=$${KAFKA_LISTENERS} --override advertised.listeners=$${KAFKA_ADVERTISED_LISTENERS} --override zookeeper.connect=$${KAFKA_ZOOKEEPER_CONNECT}"
    ]
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      LOG_DIR: "/tmp/logs"
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
```
Then run `docker-compose up` to start Kafka.

### logging-properties
In addition to the `org.jboss.logmanager:log4j-jboss-logmanager` dependency we meed to add a
`src/test/resources/logging.properties` file in order to grab the logs from the embedded broker.
The contents should be:

```
# Root logger level
logger.level=DEBUG

# Root logger handlers
logger.handlers=FILE

# File handler configuration
handler.FILE=org.jboss.logmanager.handlers.FileHandler
handler.FILE.properties=autoFlush,append,fileName
handler.FILE.autoFlush=true
handler.FILE.fileName=./target/test.log
handler.FILE.formatter=PATTERN
handler.FILE.append=true

# Formatter pattern configuration
formatter.PATTERN=org.jboss.logmanager.formatters.PatternFormatter
formatter.PATTERN.properties=pattern
formatter.PATTERN.pattern=%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n
```