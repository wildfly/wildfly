/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.api;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.record.TimestampType;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({ReactiveMessagingKafkaUserApiTestCase.CustomRunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
public class ReactiveMessagingKafkaUserApiTestCase {

    private static final long TIMEOUT = TimeoutUtil.adjust(15000);

    @Inject
    InDepthMetadataBean inDepthMetadataBean;

    @Inject
    ConfiguredToSendToTopicAndOverrideTopicForSomeMessagesBean configuredToSendToTopicAndOverrideTopicForSomeMessagesBean;

    @Inject
    NoTopicSetupOverrideForAllMessagesBean noTopicSetupOverrideForAllMessagesBean;

    @Inject
    SpecifyPartitionBean specifyPartitionBean;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-tx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingKafkaUserApiTestCase.class.getPackage())
                .addClasses(RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingKafkaUserApiTestCase.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");

        return webArchive;
    }

    /*
     * This tests that:
     * - incoming Metadata is set (also for entry 6 which did not set any metadata) and contains the topic
     * - the key is propagated from what was set in the outgoing metadata, and that it may be null when not set
     * - Headers are propagated, if set in the outgoing metadata
     * - offsets are unique per partition
     * - the timestamp and type are set, and that the timestamp matches if we set it ourselves in the outgoing metadata
     */
    @Test
    public void testOutgoingAndIncomingMetadataExtensively() throws InterruptedException {
        inDepthMetadataBean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> map = inDepthMetadataBean.getMetadatas();

        Assert.assertEquals(6, map.size());
        Map<Integer, Set<Long>> offsetsByPartition = new HashMap<>();

        for (int i = 1; i <= 6; i++) {
            IncomingKafkaRecordMetadata metadata = map.get(i);
            Assert.assertNotNull(metadata);
            if (i != 6) {
                Assert.assertEquals("KEY-" + i, metadata.getKey());
            } else {
                Assert.assertNull(metadata.getKey());
            }
            Assert.assertEquals("testing1", metadata.getTopic());
            Set<Long> offsets = offsetsByPartition.get(metadata.getPartition());
            if (offsets == null) {
                offsets = new HashSet<>();
                offsetsByPartition.put(metadata.getPartition(), offsets);
            }
            offsets.add(metadata.getOffset());
            Assert.assertNotNull(metadata.getTimestamp());
            if (i == 5) {
                Assert.assertEquals(inDepthMetadataBean.getTimestampEntry5Topic1(), metadata.getTimestamp());
            }
            Assert.assertEquals(TimestampType.CREATE_TIME, metadata.getTimestampType());
            Assert.assertNotNull(metadata.getRecord());

            Headers headers = metadata.getHeaders();
            if (i != 5) {
                Assert.assertEquals(0, headers.toArray().length);
            } else {
                Assert.assertEquals(1, headers.toArray().length);
                Header header = headers.toArray()[0];
                Assert.assertEquals("simple", header.key());
                Assert.assertArrayEquals(new byte[]{0, 1, 2}, header.value());
            }
        }
        Assert.assertEquals(6, checkOffsetsByPartitionAndCalculateTotalEntries(offsetsByPartition));
    }

    private int checkOffsetsByPartitionAndCalculateTotalEntries(Map<Integer, Set<Long>> offsetsByPartition) {
        int total = 0;
        for (Iterator<Set<Long>> it = offsetsByPartition.values().iterator(); it.hasNext() ; ) {
            Set<Long> offsets = it.next();
            long size = offsets.size();
            total += size;
            for (long l = 0; l < size; l++) {
                Assert.assertTrue(offsets.contains(l));
            }
        }
        return total;
    }

    /*
     * The outgoing method is configured to send to a given topic. We test that we can override it for certain messages
     */
    @Test
    public void testOverrideDefaultTopicWhenOneIsSetInTheConfig() throws InterruptedException {
        configuredToSendToTopicAndOverrideTopicForSomeMessagesBean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> map2 =
                configuredToSendToTopicAndOverrideTopicForSomeMessagesBean.getTesting2Metadatas();
        Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> map3 =
                configuredToSendToTopicAndOverrideTopicForSomeMessagesBean.getTesting3Metadatas();

        Assert.assertEquals(2, map2.size());
        Assert.assertEquals(2, map3.size());

        // Do some less in-depth checks here, than in the testIncomingMetadata() method, focussing on what we have set
        for (int i = 1; i <= 2; i++) {
            IncomingKafkaRecordMetadata metadata = map2.get(i);
            Assert.assertNotNull(metadata);
            Assert.assertEquals("testing2", metadata.getTopic());
        }
        for (int i = 3; i <= 4; i++) {
            IncomingKafkaRecordMetadata metadata = map3.get(i);
            Assert.assertNotNull(metadata);
            Assert.assertEquals("testing3", metadata.getTopic());
        }
    }

    /*
     * The outgoing method is configured to send to a given topic. We test that we can override it for certain messages
     */
    @Test
    public void testNoTopicConfiguredOverrideForAllMessages() throws InterruptedException {
        noTopicSetupOverrideForAllMessagesBean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> map4 =
                noTopicSetupOverrideForAllMessagesBean.getTesting4Metadatas();
        Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> map5 =
                noTopicSetupOverrideForAllMessagesBean.getTesting5Metadatas();

        Assert.assertEquals(3, map4.size());
        Assert.assertEquals(3, map5.size());

        // Do some less in-depth checks here, than in the testIncomingMetadata() method, focussing on what we have set
        for (int i = 1; i <= 6; i += 2) {
            IncomingKafkaRecordMetadata metadata = map4.get(i);
            Assert.assertNotNull(metadata);
            Assert.assertEquals("testing4", metadata.getTopic());
        }
        for (int i = 2; i <= 5; i += 2) {
            IncomingKafkaRecordMetadata metadata = map5.get(i);
            Assert.assertNotNull(metadata);
            Assert.assertEquals("testing5", metadata.getTopic());
        }
    }

    /*
     * The first 10 messages are assigned the partition by the partitioner - the last 10 specify it in the metadata.
     * There are two sets of each - the first specifies 1 as the partition for the specified ones, the second does 2.
     */
    @Test
    public void testSpecifyPartition() throws InterruptedException {
        specifyPartitionBean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);

        checkSpecifiedPartitionMetadatas(
                specifyPartitionBean.getNoPartitionSpecifiedMetadatas6(),
                specifyPartitionBean.getPartitionSpecifiedMetadatas6(),
                1);

        checkSpecifiedPartitionMetadatas(
                specifyPartitionBean.getNoPartitionSpecifiedMetadatas7(),
                specifyPartitionBean.getPartitionSpecifiedMetadatas7(),
                0);
    }

    private void checkSpecifiedPartitionMetadatas(
            Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> unspecifiedPartitions,
            Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> specifiedPartitions,
            int expectedSpecifiedPartition) {
        Assert.assertEquals(10, unspecifiedPartitions.size());
        Assert.assertEquals(10, specifiedPartitions.size());
        Set<Integer> partitionsSeen6 = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            IncomingKafkaRecordMetadata metadata = unspecifiedPartitions.get(i);
            Assert.assertNotNull(metadata);
            partitionsSeen6.add(metadata.getPartition());
        }
        // The partitioner spreads these records over the two partitions that seem to be created
        // I am missing the magic to be able to control how many partitions are set up by the embedded server,
        // currently there are two. If this check becomes problematic it can be removed
        Assert.assertTrue(partitionsSeen6.toString(), partitionsSeen6.size() > 1);

        for (int i = 11; i <= 20; i++) {
            IncomingKafkaRecordMetadata metadata = specifiedPartitions.get(i);
            Assert.assertNotNull(metadata);
            Assert.assertEquals(expectedSpecifiedPartition, metadata.getPartition());
        }
    }

    // For debugging only, as and when needed
    static String metadataToString(IncomingKafkaRecordMetadata metadata, Message<?> msg) {
        return "=====> \n" +
                "Key: " + metadata.getKey() + "\n" +
                "Topic: " + metadata.getTopic() + "\n" +
                "Offset: " + metadata.getOffset() + "\n" +
                "Partition: " + metadata.getPartition() + "\n" +
                "Timestamp: " + metadata.getTimestamp() + "\n" +
                "TimestampType" + metadata.getTimestampType() + "\n" +
                "Payload: " + msg.getPayload();
    }

    public static class CustomRunKafkaSetupTask extends RunKafkaSetupTask {
        @Override
        protected String[] getTopics() {
            return new String[]{"testing1", "testing2", "testing3", "testing4", "testing5", "testing6"};
        }

        @Override
        protected int getPartitions() {
            // Doesn't seem to have any effect. Perhaps that will change in the future
            return 10;
        }
    }
}
