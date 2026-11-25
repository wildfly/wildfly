/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link IntervalTimerMetaDataEntryMarshaller}.
 * @author Paul Ferraro
 */
public class IntervalTimerMetaDataEntryMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(MarshallingTesterFactory factory) {
        MarshalledValue<UUID, ByteBufferMarshaller> context = new ByteBufferMarshalledValueFactory(factory.getMarshaller()).createMarshalledValue(UUID.randomUUID());
        Tester<IntervalTimerMetaDataEntry<MarshalledValue<UUID, ByteBufferMarshaller>>> tester = factory.createTester(IntervalTimerMetaDataEntryMarshallerTestCase::assertEquals);
        tester.accept(new IntervalTimerMetaDataEntry<>(context, new IntervalTimerConfiguration() {
            @Override
            public Instant getStart() {
                return Instant.now();
            }
        }));
        tester.accept(new IntervalTimerMetaDataEntry<>(context, new IntervalTimerConfiguration() {
            @Override
            public Instant getStart() {
                return Instant.now();
            }

            @Override
            public Duration getInterval() {
                return Duration.ofMinutes(1);
            }
        }));
    }

    private static <V> void assertEquals(IntervalTimerMetaDataEntry<V> entry1, IntervalTimerMetaDataEntry<V> entry2) {
        Assertions.assertEquals(entry1.getContext(), entry2.getContext());
        Assertions.assertEquals(entry1.getInterval(), entry2.getInterval());
        Assertions.assertEquals(entry1.getStart(), entry2.getStart());
        Assertions.assertSame(entry1.getTimeoutMatcher(), entry2.getTimeoutMatcher());
        Assertions.assertSame(entry1.getType(), entry2.getType());
    }
}
