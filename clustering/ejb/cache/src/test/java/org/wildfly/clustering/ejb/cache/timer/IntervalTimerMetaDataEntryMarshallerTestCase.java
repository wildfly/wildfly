/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

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
 * Unit test for {@link IntervalTimerMetaDataEntryMarshaller}.
 * @author Paul Ferraro
 */
public class IntervalTimerMetaDataEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        MarshalledValue<UUID, ByteBufferMarshaller> context = new ByteBufferMarshalledValueFactory(TestProtoStreamByteBufferMarshaller.INSTANCE).createMarshalledValue(UUID.randomUUID());
        Tester<IntervalTimerMetaDataEntry<MarshalledValue<UUID, ByteBufferMarshaller>>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new IntervalTimerMetaDataEntry<>(context, new IntervalTimerConfiguration() {
            @Override
            public Instant getStart() {
                return Instant.now();
            }
        }), IntervalTimerMetaDataEntryMarshallerTestCase::assertEquals);
        tester.test(new IntervalTimerMetaDataEntry<>(context, new IntervalTimerConfiguration() {
            @Override
            public Instant getStart() {
                return Instant.now();
            }

            @Override
            public Duration getInterval() {
                return Duration.ofMinutes(1);
            }
        }), IntervalTimerMetaDataEntryMarshallerTestCase::assertEquals);
    }

    private static <V> void assertEquals(IntervalTimerMetaDataEntry<V> entry1, IntervalTimerMetaDataEntry<V> entry2) {
        Assert.assertEquals(entry1.getContext(), entry2.getContext());
        Assert.assertEquals(entry1.getInterval(), entry2.getInterval());
        Assert.assertEquals(entry1.getStart(), entry2.getStart());
        Assert.assertSame(entry1.getTimeoutMatcher(), entry2.getTimeoutMatcher());
        Assert.assertSame(entry1.getType(), entry2.getType());
    }
}
