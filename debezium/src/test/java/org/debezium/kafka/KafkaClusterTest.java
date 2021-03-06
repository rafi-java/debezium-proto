/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.kafka;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.debezium.Testing;
import org.debezium.util.Stopwatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Randall Hauch
 *
 */
public class KafkaClusterTest {

    private KafkaCluster cluster;
    private File dataDir;

    @Before
    public void beforeEach() {
        dataDir = Testing.Files.createTestingDirectory("cluster");
        Testing.Files.delete(dataDir);
        cluster = new KafkaCluster().usingDirectory(dataDir);
    }

    @After
    public void afterEach() {
        cluster.shutdown();
        Testing.Files.delete(dataDir);
    }

    @Test
    public void shouldStartClusterWithOneBrokerAndRemoveData() throws Exception {
        cluster.deleteDataUponShutdown(true).addBrokers(1).startup();
        cluster.onEachDirectory(this::assertValidDataDirectory);
        cluster.shutdown();
        cluster.onEachDirectory(this::assertDoesNotExist);
    }

    @Test
    public void shouldStartClusterWithMultipleBrokerAndRemoveData() throws Exception {
        cluster.deleteDataUponShutdown(true).addBrokers(3).startup();
        cluster.onEachDirectory(this::assertValidDataDirectory);
        cluster.shutdown();
        cluster.onEachDirectory(this::assertDoesNotExist);
    }

    @Test
    public void shouldStartClusterWithOneBrokerAndLeaveData() throws Exception {
        cluster.deleteDataUponShutdown(false).addBrokers(1).startup();
        cluster.onEachDirectory(this::assertValidDataDirectory);
        cluster.shutdown();
        cluster.onEachDirectory(this::assertValidDataDirectory);
    }

    @Test
    public void shouldStartClusterWithMultipleBrokerAndLeaveData() throws Exception {
        cluster.deleteDataUponShutdown(false).addBrokers(3).startup();
        cluster.onEachDirectory(this::assertValidDataDirectory);
        cluster.shutdown();
        cluster.onEachDirectory(this::assertValidDataDirectory);
    }

    @Test
    public void shouldStartClusterAndAllowProducersAndConsumersToUseIt() throws Exception {
        Testing.Debug.enable();
        final String topicName = "topicA";
        final CountDownLatch completion = new CountDownLatch(2);
        final int numMessages = 100;
        final AtomicLong messagesRead = new AtomicLong(0);

        // Start a cluster and create a topic ...
        cluster.deleteDataUponShutdown(false).addBrokers(1).startup();
        cluster.createTopics(topicName);

        // Consume messages asynchronously ...
        Stopwatch sw = Stopwatch.reusable().start();
        cluster.useTo().consumeIntegers(topicName, numMessages, 10, TimeUnit.SECONDS, completion::countDown, (key, value) -> {
            messagesRead.incrementAndGet();
            return true;
        });

        // Produce some messages asynchronously ...
        cluster.useTo().produceIntegers(topicName, numMessages, 1, completion::countDown);

        // Wait for both to complete ...
        if (completion.await(10, TimeUnit.SECONDS)) {
            sw.stop();
            Testing.debug("Both consumer and producer completed normally in " + sw.durations());
        } else {
            Testing.debug("Consumer and/or producer did not completed normally");
        }

        assertThat(messagesRead.get()).isEqualTo(numMessages);
    }

    @Test
    public void shouldStartClusterAndAllowInteractiveProductionAndAutomaticConsumersToUseIt() throws Exception {
        Testing.Debug.enable();
        final String topicName = "topicA";
        final CountDownLatch completion = new CountDownLatch(1);
        final int numMessages = 3;
        final AtomicLong messagesRead = new AtomicLong(0);

        // Start a cluster and create a topic ...
        cluster.deleteDataUponShutdown(false).addBrokers(1).startup();
        cluster.createTopics(topicName);

        // Consume messages asynchronously ...
        Stopwatch sw = Stopwatch.reusable().start();
        cluster.useTo().consumeIntegers(topicName, numMessages, 10, TimeUnit.SECONDS, completion::countDown, (key, value) -> {
            messagesRead.incrementAndGet();
            return true;
        });

        // Produce some messages interactively ...
        cluster.useTo()
               .createProducer("manual", new StringSerializer(), new IntegerSerializer())
               .write(topicName, "key1", 1)
               .write(topicName, "key2", 2)
               .write(topicName, "key3", 3)
               .close();

        // Wait for the consumer to to complete ...
        if (completion.await(10, TimeUnit.SECONDS)) {
            sw.stop();
            Testing.debug("The consumer completed normally in " + sw.durations());
        } else {
            Testing.debug("Consumer did not completed normally");
        }

        assertThat(messagesRead.get()).isEqualTo(numMessages);
    }

    @Test
    public void shouldStartClusterAndAllowAsynchronousProductionAndAutomaticConsumersToUseIt() throws Exception {
        Testing.Debug.enable();
        final String topicName = "topicA";
        final CountDownLatch completion = new CountDownLatch(2);
        final int numMessages = 3;
        final AtomicLong messagesRead = new AtomicLong(0);

        // Start a cluster and create a topic ...
        cluster.deleteDataUponShutdown(false).addBrokers(2).startup();
        cluster.createTopics(topicName);

        // Consume messages asynchronously ...
        Stopwatch sw = Stopwatch.reusable().start();
        cluster.useTo().consumeIntegers(topicName, numMessages, 10, TimeUnit.SECONDS, completion::countDown, (key, value) -> {
            messagesRead.incrementAndGet();
            return true;
        });

        // Produce some messages interactively ...
        cluster.useTo().produce("manual", new StringSerializer(), new IntegerSerializer(), produer -> {
            produer.write(topicName, "key1", 1);
            produer.write(topicName, "key2", 2);
            produer.write(topicName, "key3", 3);
            completion.countDown();
        });

        // Wait for the consumer to to complete ...
        if (completion.await(10, TimeUnit.SECONDS)) {
            sw.stop();
            Testing.debug("The consumer completed normally in " + sw.durations());
        } else {
            Testing.debug("Consumer did not completed normally");
        }
        assertThat(messagesRead.get()).isEqualTo(numMessages);
    }

    protected void assertValidDataDirectory(File dir) {
        assertThat(dir.exists()).isTrue();
        assertThat(dir.isDirectory()).isTrue();
        assertThat(dir.canWrite()).isTrue();
        assertThat(dir.canRead()).isTrue();
        assertThat(Testing.Files.inTargetDir(dir)).isTrue();
    }

    protected void assertDoesNotExist(File dir) {
        assertThat(dir.exists()).isFalse();
    }
}
