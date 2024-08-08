/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link ScheduleTimerMetaDataEntryMarshaller}.
 * @author Paul Ferraro
 */
public class ScheduleTimerMetaDataEntryMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(MarshallingTesterFactory factory) throws NoSuchMethodException {
        MarshalledValue<UUID, ByteBufferMarshaller> context = new ByteBufferMarshalledValueFactory(factory.getMarshaller()).createMarshalledValue(UUID.randomUUID());
        Tester<ScheduleTimerMetaDataEntry<MarshalledValue<UUID, ByteBufferMarshaller>>> tester = factory.createTester(ScheduleTimerMetaDataEntryMarshallerTestCase::assertEquals);
        tester.accept(new ScheduleTimerMetaDataEntry<>(context, new ScheduleTimerConfiguration() {
            @Override
            public ImmutableScheduleExpression getScheduleExpression() {
                return ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().get();
            }
        }));
        Instant start = Instant.now();
        Instant end = start.plus(Duration.ofDays(10));
        tester.accept(new ScheduleTimerMetaDataEntry<>(context, new ScheduleTimerConfiguration() {
            @Override
            public ImmutableScheduleExpression getScheduleExpression() {
                return ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().start(start).end(end).year("1970").month("1").dayOfMonth("1").dayOfWeek("1").zone(ZoneId.of("GMT")).hour("0").minute("0").second("0").get();
            }
        }, ScheduleTimerMetaDataEntryMarshallerTestCase.class.getDeclaredMethod("timeout")));
        tester.accept(new ScheduleTimerMetaDataEntry<>(context, new ScheduleTimerConfiguration() {
            @Override
            public ImmutableScheduleExpression getScheduleExpression() {
                return ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().start(start).end(end).year("1970").month("JAN").dayOfMonth("1").dayOfWeek("TUES").zone(ZoneId.of("America/New_York")).hour("0").minute("0").second("0").get();
            }
        }, ScheduleTimerMetaDataEntryMarshallerTestCase.class.getDeclaredMethod("ejbTimeout")));
        tester.accept(new ScheduleTimerMetaDataEntry<>(context, new ScheduleTimerConfiguration() {
            @Override
            public ImmutableScheduleExpression getScheduleExpression() {
                return ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().start(start).end(end).year("1970").month("JAN").dayOfMonth("1").dayOfWeek("TUES").zone(ZoneId.of("America/New_York")).hour("0").minute("0").second("0").get();
            }
        }, ScheduleTimerMetaDataEntryMarshallerTestCase.class.getDeclaredMethod("ejbTimeout", Object.class)));
    }

    void timeout() {
    }

    void ejbTimeout() {
    }

    void ejbTimeout(Object timer) {
    }

    private static <V> void assertEquals(ScheduleTimerMetaDataEntry<V> entry1, ScheduleTimerMetaDataEntry<V> entry2) {
        Assertions.assertEquals(entry1.getContext(), entry2.getContext());
        Assertions.assertEquals(entry1.getStart(), entry2.getStart());
        Assertions.assertEquals(entry1.getTimeoutMatcher(), entry2.getTimeoutMatcher());
        Assertions.assertSame(entry1.getType(), entry2.getType());
        Assertions.assertEquals(entry1.getScheduleExpression().getStart(), entry2.getScheduleExpression().getStart());
        Assertions.assertEquals(entry1.getScheduleExpression().getEnd(), entry2.getScheduleExpression().getEnd());
        Assertions.assertEquals(entry1.getScheduleExpression().getYear(), entry2.getScheduleExpression().getYear());
        Assertions.assertEquals(entry1.getScheduleExpression().getMonth(), entry2.getScheduleExpression().getMonth());
        Assertions.assertEquals(entry1.getScheduleExpression().getDayOfMonth(), entry2.getScheduleExpression().getDayOfMonth());
        Assertions.assertEquals(entry1.getScheduleExpression().getDayOfWeek(), entry2.getScheduleExpression().getDayOfWeek());
        Assertions.assertEquals(entry1.getScheduleExpression().getZone(), entry2.getScheduleExpression().getZone());
        Assertions.assertEquals(entry1.getScheduleExpression().getHour(), entry2.getScheduleExpression().getHour());
        Assertions.assertEquals(entry1.getScheduleExpression().getMinute(), entry2.getScheduleExpression().getMinute());
        Assertions.assertEquals(entry1.getScheduleExpression().getSecond(), entry2.getScheduleExpression().getSecond());
    }
}
