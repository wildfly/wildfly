/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.protostream.TestProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * @author Paul Ferraro
 */
public class IntervalTimerCreationMetaDataEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        MarshalledValue<UUID, ByteBufferMarshaller> context = new ByteBufferMarshalledValueFactory(TestProtoStreamByteBufferMarshaller.INSTANCE).createMarshalledValue(UUID.randomUUID());
        Tester<IntervalTimerCreationMetaDataEntry<MarshalledValue<UUID, ByteBufferMarshaller>>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new IntervalTimerCreationMetaDataEntry<>(context, new IntervalTimerConfiguration() {
            @Override
            public Instant getStart() {
                return Instant.now();
            }
        }), IntervalTimerCreationMetaDataEntryMarshallerTestCase::assertEquals);
        tester.test(new IntervalTimerCreationMetaDataEntry<>(context, new IntervalTimerConfiguration() {
            @Override
            public Instant getStart() {
                return Instant.now();
            }

            @Override
            public Duration getInterval() {
                return Duration.ofMinutes(1);
            }
        }), IntervalTimerCreationMetaDataEntryMarshallerTestCase::assertEquals);
    }

    private static <V> void assertEquals(IntervalTimerCreationMetaDataEntry<V> entry1, IntervalTimerCreationMetaDataEntry<V> entry2) {
        Assert.assertEquals(entry1.getContext(), entry2.getContext());
        Assert.assertEquals(entry1.getInterval(), entry2.getInterval());
        Assert.assertEquals(entry1.getStart(), entry2.getStart());
        Assert.assertEquals(entry1.getTimeoutMatcher(), entry2.getTimeoutMatcher());
        Assert.assertSame(entry1.getType(), entry2.getType());
    }
}
