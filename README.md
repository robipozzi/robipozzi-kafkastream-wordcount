# Kafka Streams WordCount demo application
- [Introduction](#introduction)
- [Setup and run Kafka](#setup-and-run-kafka)
    - [Run Kafka cluster on local environment](#run-kafka-cluster-on-local-environment)
    - [Run Kafka cluster on Confluent](#run-kafka-cluster-on-confluent)
    - [Create, delete and describe Kafka topics](#create-delete-and-describe-kafka-topics)
    - [Producers and consumers using Kafka command line tools](#producers-and-consumers-using-kafka-command-line-tools)
- [How the application works](#how-the-application-works)
    
## Introduction
This repository holds the code for experimentations on Kafka Streams technology.

The code provided in this repository combines in one single application the three applications described in the tutorial available in the Kafka Streams official documentation https://kafka.apache.org/36/documentation/streams/tutorial.

The application codebase implements the following 3 scenarios:
* **Pipe messages** - reads records from *streams-plaintext-input* topic and just send it as-is to the other topic *streams-pipe-output*;
* **Line Split** - reads records from *streams-plaintext-input* topic, apply Kafka Streams transformation to split the message value string into a list of words, producing each word as a new record to the output stream *streams-linesplit-output*;
* **Word Count** - reads records from *streams-plaintext-input* topic, applies several Kafka Streams transformations to split the message value string into a list of words, count each word occurence and publish it to *streams-wordcount-output* topic.

To access the code, open a Terminal and start by cloning this repository with the following commands:

```
mkdir $HOME/dev
cd $HOME/dev
git clone https://github.com/robipozzi/robipozzi-kafkastreams-wordcount
```

## Setup and run Kafka
To see how Kafka Streams works, reading what Kafka producers published to some input topic and publish the result of a chain of transformations to an ouput topic, you will need to setup a few things in advance, such as a Kafka cluster to interact with and Kafka topics to produce and consume messages; also it could be useful to have some Kafka consumers to test that the messages have been correctly produced by our Kafka Streams application: everything is 
already described in details in this GitHub repository https://github.com/robipozzi/robipozzi-kafka, you will find pointers to the appropriate content in the 
paragraphs below.

### Run Kafka cluster on local environment
One option to run a Kafka cluster is obviously installing and running locally, please refer to 
https://github.com/robipozzi/robipozzi-kafka#run-Kafka-cluster-on-local-environment for all the details.

### Run Kafka cluster on Confluent
Another option to setup a Kafka cluster is to use a Cloud solution, for instance Confluent (https://www.confluent.io/), you can refer to 
https://github.com/robipozzi/robipozzi-kafka#run-Kafka-cluster-on-confluent for details regarding this option.

### Create, delete and describe Kafka topics
Once the Kafka cluster has been setup, you can find details on how to manage topics (i.e.: create, delete, ...) at 
https://github.com/robipozzi/robipozzi-kafka#create-delete-and-describe-kafka-topics

### Producers and consumers using Kafka command line tools
Kafka provides command line tools to create and run producers and consumers: in my other repository https://github.com/robipozzi/robipozzi-kafka I developed some convenient script to run Kafka producer and consumer for this specific WordCount application.

Open a terminal and run the following commands to download the scripts: 

```
mkdir $HOME/dev
cd $HOME/dev
git clone https://github.com/robipozzi/robipozzi-kafka
```

These three scripts are the relevant ones for the WordCount application: 
* **[start-producer.sh](https://github.com/robipozzi/robipozzi-kafka/blob/main/samples/streams-wordcount/start-producer.sh)** runs a Kafka producer that allows to write sentences in a terminal and publish them as messages to *streams-plaintext-input* Kafka topic. Open a new terminal and run the following commands to start the Kafka producer

```
cd $HOME/dev/robipozzi-kafka/samples/streams-wordcount
./start-producer.sh
```

* **[start-consumer.sh](https://github.com/robipozzi/robipozzi-kafka/blob/main/samples/streams-wordcount/start-consumer.sh)** runs a specific Kafka consumer, with **value.deserializer** set to non default **org.apache.kafka.common.serialization.LongDeserializer** to allow reading from *streams-wordcount-output* Kafka topic where words count will be produced by the application. Open a new terminal and run the following commands to start the Kafka consumer

```
cd $HOME/dev/robipozzi-kafka/samples/streams-wordcount
./start-consumer.sh
```

* **[test-consumer.sh](https://github.com/robipozzi/robipozzi-kafka/blob/main/test-consumer.sh)** runs a Kafka consumer that allows to connect to a Kafka cluster and then select a generic Kafka topic, other than *streams-wordcount-output*, to read messages from. Open a new terminal and run the following commands to start the Kafka consumer

```
cd $HOME/dev/robipozzi-kafka/
./test-consumer.sh
```

## How the application works
The application uses Kafka Streams DSL to implement the scenarios described in [Introduction](#introduction).
 
The code for this application is based on:
- **Maven**: here is the **[POM](pom.xml)** that defines project configuration; the library dependencies section is reported here below

```
<dependencies>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter</artifactId>
	</dependency>
	<dependency>
		<groupId>org.apache.kafka</groupId>
		<artifactId>kafka-streams</artifactId>
	</dependency>
	<dependency>
		<groupId>org.apache.kafka</groupId>
		<artifactId>kafka-clients</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-test</artifactId>
		<scope>test</scope>
	</dependency>
</dependencies>
```
	
- **Spring Boot 3.1.2**: the usage of Spring Boot framework v3.1.2, with all its implicit dependencies, is declared in the same **[POM](pom.xml)**; 
as any Spring Boot application, it has a specific configuration file called **[application.properties](src/main/resources/application.properties)**

```
<parent>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-parent</artifactId>
	<version>3.1.2</version>
	<relativePath/> <!-- lookup parent from repository -->
</parent>
```

- **Kafka and Kafka Streams libraries**: they are injected as Spring dependencies, as it can be seen in the **[POM](pom.xml)** dependencies section.

Every Spring Boot application needs to have a main class annotated as **@SpringBootApplication**; our application main class is 
**[KafkaStreamsWordCount](src/main/java/com/rpozzi/kafkastreams/KafkaStreamsWordCount.java)**, whose code is reported here below for reference

```
@SpringBootApplication
@ComponentScan(basePackages = { "com.rpozzi.kafkastreams" })
public class KafkaStreamsWordCount {
	private static final Logger logger = LoggerFactory.getLogger(KafkaStreamsWordCount.class);
	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String kafkaBootstrapServers;
	@Autowired
	private WordCountStreamsService wordCountStreamsSrv;

	public static void main(String[] args) {
		SpringApplication.run(KafkaStreamsWordCount.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			logger.debug("Let's inspect the beans provided by Spring Boot:");
			logger.debug("************** Spring Boot beans - START **************");
			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String beanName : beanNames) {
				logger.debug(beanName);
			}
			logger.debug("************** Spring Boot beans - END **************");
			
			logger.debug("Print application configuration parameters");
			logger.debug("************** Application configuration parameters - START **************");
			logger.debug("Kafka Bootstrap Servers :  " + kafkaBootstrapServers);
			logger.debug("************** Application configuration parameters - END **************");
			
			logger.info("Application " + ctx.getId() + " started !!!");

			// ############### Kafka Streams - Word count streams service ###############
			wordCountStreamsSrv.process();
		};
	}

}
```

It is out of scope of this doc to explain in detail how Spring Boot works, let's just say that once the application is started via *main()* method, the *commandLineRunner()* method is kicked in, where **wordCountStreamsSrv.process()** is called.

But where **wordCountStreamsSrv** comes from? It is an instance of 
**[WordCountStreamsService](src/main/java/com/rpozzi/kafkastreams/service/WordCountStreamsService.java)** class, whose code is reported below 
for reference, injected into *KafkaStreamsWordCount* with the following Spring Boot annotation

```
@Autowired
private WordCountStreamsService wordCountStreamsSrv;
```

The **[WordCountStreamsService](src/main/java/com/rpozzi/kafkastreams/service/WordCountStreamsService.java)** class has a *process()* method 
where Kafka Streams DSL is used to create and run a Stream Processor Topology that does the following:

* reads input records from *streams-plaintext-input* topic and publish as-is to *streams-pipe-output* topic
* reads input records from *streams-plaintext-input* topic, apply **flatMapValues** transformation to split sentences and publish words to *streams-linesplit-output* topic
* reads input records from *streams-plaintext-input* topic, apply several transformation (i.e.: **flatMapValues**, **groupByKey** and **count**) to split sentences into words, count each word occurence and publish to *streams-wordcount-output* topic

Let's see how Kafka Streams works, stepping into *process()* method line by line.

First we need to create a **java.util.Properties** instance and populate it with appropriate info to:

- instruct the application to connect to a Kafka cluster (see how *StreamsConfig.BOOTSTRAP_SERVERS_CONFIG* key is set with the value of Kafka Bootstrap Servers URL);
- define Kafka messages key and value Serializer and Deserializer.

```
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaStreamsAppId);
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
```

Then, Kafka Streams DSL requires to:

- instantiate a **StreamsBuilder** class;
- create an instance of **KStream**, calling the *StreamsBuilder.stream(<kafka_topic>)* method

```
// Initialize StreamsBuilder
final StreamsBuilder builder = new StreamsBuilder();
		
// Read Stream from input Kafka Topic ${kafka.topic.plaintextinput} (see application.properties for mapping)
KStream<String, String> source = builder.stream(plainTextKafkaTopic);
```

The above code creates a **KStream** instance called *source*, bound to *streams-plaintext-input* Kafka topic: the data published to that topic are interpreted as a record stream and several transformations can then be applied to it, using Kafka Streams DSL constructs.

* **Pipe** scenario

Pipe just takes records from *source* **KStream** instantiated above and publish message to another topic, see code snippet below
 
```
// =============== PIPE - START ===============
// Pipe source stream's record as-is to output Kafka Topic ${kafka.topic.pipeoutput} (see application.properties for mapping)
source.to(pipeOutputKafkaTopic);
// =============== PIPE - END ===============
```

* **Split Line** scenario

In this second scenario, as it can be seen in the code snippet below, sentences coming in as messages to the *streams-plaintext-input* input Kafka topic are split into words, via **Arrays.asList(value.split("\\W+"))** method. The **flatMapValues** Kafka Streams stateless transformation is then applied.

The **flatMapValues** stateless transformation takes one record and produces zero, one, or more records, with possibly changing value and value type, while retaining the key of the original record.

A new stream *words* is created by **flatMapValues** transformation and the record data are then flushed to *streams-linesplit-output* output topic, using **words.to(lineSplitKafkaTopic)** method.
 
```
// =============== LINE SPLIT - START ===============
// The operator takes the source stream as its input, and generates a new stream
// named words by processing each record from the source stream in order and breaking its value string into a list of words,
// producing each word as a new record to the output words stream.
// This is a stateless operator that does not need to keep track of any previously received records or processed results.
KStream<String, String> words = source.flatMapValues(value -> Arrays.asList(value.split("\\W+")));
// We then write the word stream back into another Kafka Topic ${kafka.topic.linesplitoutput}
words.to(lineSplitKafkaTopic);
// =============== LINE SPLIT - END ===============
```

* **Word Count** scenario

In this third scenario, several transformations are applied sequentially to count each word occurence, as implemented in the code snippet below

```
// =============== WORD COUNT - START ===============
source.flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\W+")))
	.groupBy((key, value) -> value)
	.count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"))
	.toStream()
	.to(wordCountKafkaTopic, Produced.with(Serdes.String(), Serdes.Long()));
// =============== WORD COUNT - END ===============
```

The code above does the following
1. applies **flatMapValues** to change message value (using **Arrays.asList(value.split("\\W+"))** to split sentence in its composing words) 
2. applies **groupBy** stateless transformation to group words
3. apply **count** transformation to count each word (since *count* is a stateful transformation, it has "memory" of all the word occurences and keeps accumulating)
4. call **toStream()** method to stream the transformed records to a new KStream
5. call **to()** method to publish record data to *streams-wordcount-output* output topic