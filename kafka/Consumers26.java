package it.polimi.nsds.kafka.eval;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Group number: 26
// Group members: Christian Mariano, Armando Fiorini, MohammadAmin Rahimi

// Number of partitions for inputTopic (min, max): (1, n)
// Number of partitions for outputTopic1 (min, max): (1, n)
// Number of partitions for outputTopic2 (min, max): (1, n)

// Number of instances of Consumer1 (and groupId of each instance) (min, max): (1, 1) groupId: GroupA
// Number of instances of Consumer2 (and groupId of each instance) (min, max): (1, n) groupId: GroupB

// Please, specify below any relation between the number of partitions for the topics
// and the number of instances of each Consumer

//We must have one input partition different for each instances of consumer in the same logic group

public class Consumers26 {
    public static void main(String[] args) {
        String serverAddr = "localhost:9092";
        // int consumerId = Integer.valueOf(args[0]);
        int consumerId = 2;
        // String groupId = args[1];
        String groupId = "GroupB";
        if (consumerId == 1) {
            Consumer1 consumer = new Consumer1(serverAddr, groupId);
            consumer.execute();
        } else if (consumerId == 2) {
            Consumer2 consumer = new Consumer2(serverAddr, groupId);
            consumer.execute();
        }
    }

    private static class Consumer1 {
        private final String serverAddr;
        private final String consumerGroupId;

        private static final String inputTopic = "inputTopic";
        private static final String intermediatedTopic = "intermTopic";
        private static final String outputTopic = "outputTopic1";
        private static final String producerTransactionalId = UUID.randomUUID().toString();


        public Consumer1(String serverAddr, String consumerGroupId) {
            this.serverAddr = serverAddr;
            this.consumerGroupId = consumerGroupId;
        }

        public void execute() {
            // Consumer
            final Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(false));
            // TODO: add properties if needed

            KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singletonList(inputTopic));

            // Producer
            final Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
            producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, producerTransactionalId);
            producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, String.valueOf(true));
            // TODO: add properties if needed

            final KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProps);
            producer.initTransactions();
            // TODO: add code if needed
            
            int sum = 0;
            int counter = 0;
            while (true) {
                final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
                producer.beginTransaction();
                for (final ConsumerRecord<String, Integer> record : records) {
                    // TODO: add code to process records
                    final String key = record.key();
                    final int value = record.value();
                    if(counter < 10) {
                        sum += value;
                        counter++;
                    } else if (counter == 10) {
                        System.out.println(sum);
                        producer.send(new ProducerRecord<>(outputTopic, "sum", sum));
                        sum = 0;
                        counter = 0;

                        // The producer manually commits the offsets for the consumer within the transaction
                        final Map<TopicPartition, OffsetAndMetadata> map = new HashMap<>();
                        for (final TopicPartition partition : records.partitions()) {
                            final List<ConsumerRecord<String, Integer>> partitionRecords = records.records(partition);
                            final long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                            map.put(partition, new OffsetAndMetadata(lastOffset + 1));
                        }

                        //this method is used to send the offsets to update them
                        producer.sendOffsetsToTransaction(map, consumer.groupMetadata());
                    }
                    // if(record.offset())
                    // producer.send(new ProducerRecord<>(outputTopic, key, ))
                }
                    producer.commitTransaction();
            }
        }
    }

    private static class Consumer2 {
        private final String serverAddr;
        private final String consumerGroupId;

        private static final String inputTopic = "inputTopic";
        private static final String outputTopic = "outputTopic2";
        private static final boolean autoCommit = true;
        private static final int autoCommitIntervalMs = 15000;

        private static final String offsetResetStrategy = "latest";

        public Consumer2(String serverAddr, String consumerGroupId) {
            this.serverAddr = serverAddr;
            this.consumerGroupId = consumerGroupId;
        }

        public void execute() {
            // Consumer
            final Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
            consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(autoCommit)); 
            consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, String.valueOf(autoCommitIntervalMs));

            consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetResetStrategy);

            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());

            KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singletonList(inputTopic));

            // Producer
            final Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());

            final KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProps);

            //first element of the list is the count and the second is the sum
            HashMap<String, List<Integer>> sumCountsMap = new HashMap<>(); 
            while (true) {
                final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
                for (final ConsumerRecord<String, Integer> record : records) {
                    final String key = record.key();
                    final int value = record.value();
                    if(!sumCountsMap.containsKey(key)) {
                        sumCountsMap.put(key, new ArrayList<Integer>(2));
                        sumCountsMap.get(key).add(1);
                        sumCountsMap.get(key).add(value);
                    } else {
                        final int currentCount = sumCountsMap.get(key).get(0);
                        final int currentSum = sumCountsMap.get(key).get(1);
                        sumCountsMap.get(key).set(0, currentCount+1);
                        sumCountsMap.get(key).set(1, currentSum+value);
                        System.out.println("Current count2: " + sumCountsMap.get(key).get(0));
                        if(sumCountsMap.get(key).get(0) == 10) {
                            System.out.println("Key: " + key + " Sum: " + sumCountsMap.get(key).get(1));
                            producer.send(new ProducerRecord<>(outputTopic, key, sumCountsMap.get(key).get(1)));
                            sumCountsMap.get(key).set(0, 0);
                            sumCountsMap.get(key).set(1, 0);
                        }
                    }
                }

            }
        }
    }
}