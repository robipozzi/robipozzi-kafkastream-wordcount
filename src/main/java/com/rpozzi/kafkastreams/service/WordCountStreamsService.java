package com.rpozzi.kafkastreams.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WordCountStreamsService {
	private static final Logger logger = LoggerFactory.getLogger(WordCountStreamsService.class);
	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String kafkaBootstrapServers;
	@Value(value = "${kafkastreams.application.id}")
	private String kafkaStreamsAppId;
	@Value(value = "${kafka.topic.plaintextinput}")
	private String plainTextKafkaTopic;
	@Value(value = "${kafka.topic.pipeoutput}")
	private String pipeOutputKafkaTopic;
	@Value(value = "${kafka.topic.linesplitoutput}")
	private String lineSplitKafkaTopic;
	@Value(value = "${kafka.topic.wordcountoutput}")
	private String wordCountKafkaTopic;

	public void process() {
		// ########################################################################
		// ############### Kafka Streams - Word counter sample code ###############
		// ########################################################################
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaStreamsAppId);
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

		// Since each of the source stream's record is a String typed key-value pair,
		// let's treat the value string as a text line and split it into words with a FlatMapValues operator
		
		// Initialize StreamsBuilder
		final StreamsBuilder builder = new StreamsBuilder();
		
		// Read Stream from input Kafka Topic ${kafka.topic.plaintextinput} (see application.properties for mapping)
		logger.info("Streaming from '" + plainTextKafkaTopic + "' Kafka topic ...");
		KStream<String, String> source = builder.stream(plainTextKafkaTopic);
		
		// =============== PIPE - START ===============
		// Pipe source stream's record as-is to output Kafka Topic ${kafka.topic.pipeoutput} (see application.properties for mapping)
		logger.info("Pipe to '" + pipeOutputKafkaTopic + "' Kafka topic ...");
		source.to(pipeOutputKafkaTopic);
		// =============== PIPE - END ===============
		
		// =============== LINE SPLIT - START ===============
		// The operator takes the source stream as its input, and generates a new stream
		// named words by processing each record from the source stream in order and breaking its value string into a list of words,
		// producing each word as a new record to the output words stream.
		// This is a stateless operator that does not need to keep track of any previously received records or processed results.
		KStream<String, String> words = source.flatMapValues(value -> Arrays.asList(value.split("\\W+")));
		// We then write the word stream back into another Kafka Topic ${kafka.topic.linesplitoutput}
		logger.info("Send split words to '" + lineSplitKafkaTopic + "' Kafka topic ...");
		words.to(lineSplitKafkaTopic);
		// =============== LINE SPLIT - END ===============
		
		// =============== WORD COUNT - START ===============
		source.flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\W+")))
			.groupBy((key, value) -> value)
			.count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"))
			.toStream()
			.to(wordCountKafkaTopic, Produced.with(Serdes.String(), Serdes.Long()));
		// =============== WORD COUNT - END ===============

		final Topology topology = builder.build();
		logger.debug("Printing Topology ...");
		logger.debug(topology.describe().toString());
		final KafkaStreams streams = new KafkaStreams(topology, props);
		final CountDownLatch latch = new CountDownLatch(1);

		// attach shutdown handler to catch control-c
		Runtime.getRuntime().addShutdownHook(new Thread("wordcount-streams-shutdown-hook") {
			@Override
			public void run() {
				streams.close();
				latch.countDown();
			}
		});

		try {
			streams.start();
			latch.await();
		} catch (Throwable e) {
			System.exit(1);
		}
		System.exit(0);
	}
}