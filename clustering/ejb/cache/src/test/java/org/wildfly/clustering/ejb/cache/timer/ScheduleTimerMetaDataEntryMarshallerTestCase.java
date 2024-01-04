/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.protostream.TestProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * Unit test for {@link ScheduleTimerMetaDataEntryMarshaller}.
 * @author Paul Ferraro
 */
public class ScheduleTimerMetaDataEntryMarshallerTestCase {

    @Test
    public void test() throws IOException, NoSuchMethodException {
        MarshalledValue<UUID, ByteBufferMarshaller> context = new ByteBufferMarshalledValueFactory(TestProtoStreamByteBufferMarshaller.INSTANCE).createMarshalledValue(UUID.randomUUID());
        Tester<ScheduleTimerMetaDataEntry<MarshalledValue<UUID, ByteBufferMarshaller>>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ScheduleTimerMetaDataEntry<>(context, new ScheduleTimerConfiguration() {
            @Override
            public ImmutableScheduleExpression getScheduleExpression() {
                return ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().get();
            }
        }), ScheduleTimerMetaDataEntryMarshallerTestCase::assertEquals);
        Instant start = Instant.now();
        Instant end = start.plus(Duration.ofDays(10));
        tester.test(new ScheduleTimerMetaDataEntry<>(context, new ScheduleTimerConfiguration() {
            @Override
            public ImmutableScheduleExpression getScheduleExpression() {
                return ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().start(start).end(end).year("1970").month("1").dayOfMonth("1").dayOfWeek("1").zone(ZoneId.of("GMT")).hour("0").minute("0").second("0").get();
            }
        }, ScheduleTimerMetaDataEntryMarshallerTestCase.class.getDeclaredMethod("test")), ScheduleTimerMetaDataEntryMarshallerTestCase::assertEquals);
        tester.test(new ScheduleTimerMetaDataEntry<>(context, new ScheduleTimerConfiguration() {
            @Override
            public ImmutableScheduleExpression getScheduleExpression() {
                return ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().start(start).end(end).year("1970").month("JAN").dayOfMonth("1").dayOfWeek("TUES").zone(ZoneId.of("America/New_York")).hour("0").minute("0").second("0").get();
            }
        }, ScheduleTimerMetaDataEntryMarshallerTestCase.class.getDeclaredMethod("ejbTimeout")), ScheduleTimerMetaDataEntryMarshallerTestCase::assertEquals);
        tester.test(new ScheduleTimerMetaDataEntry<>(context, new ScheduleTimerConfiguration() {
            @Override
            public ImmutableScheduleExpression getScheduleExpression() {
                return ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().start(start).end(end).year("1970").month("JAN").dayOfMonth("1").dayOfWeek("TUES").zone(ZoneId.of("America/New_York")).hour("0").minute("0").second("0").get();
            }
        }, ScheduleTimerMetaDataEntryMarshallerTestCase.class.getDeclaredMethod("ejbTimeout", Object.class)), ScheduleTimerMetaDataEntryMarshallerTestCase::assertEquals);
    }

    void ejbTimeout() {
    }

    void ejbTimeout(Object timer) {
    }

    private static <V> void assertEquals(ScheduleTimerMetaDataEntry<V> entry1, ScheduleTimerMetaDataEntry<V> entry2) {
        Assert.assertEquals(entry1.getContext(), entry2.getContext());
        Assert.assertEquals(entry1.getStart(), entry2.getStart());
        Assert.assertEquals(entry1.getTimeoutMatcher(), entry2.getTimeoutMatcher());
        Assert.assertSame(entry1.getType(), entry2.getType());
        Assert.assertEquals(entry1.getScheduleExpression().getStart(), entry2.getScheduleExpression().getStart());
        Assert.assertEquals(entry1.getScheduleExpression().getEnd(), entry2.getScheduleExpression().getEnd());
        Assert.assertEquals(entry1.getScheduleExpression().getYear(), entry2.getScheduleExpression().getYear());
        Assert.assertEquals(entry1.getScheduleExpression().getMonth(), entry2.getScheduleExpression().getMonth());
        Assert.assertEquals(entry1.getScheduleExpression().getDayOfMonth(), entry2.getScheduleExpression().getDayOfMonth());
        Assert.assertEquals(entry1.getScheduleExpression().getDayOfWeek(), entry2.getScheduleExpression().getDayOfWeek());
        Assert.assertEquals(entry1.getScheduleExpression().getZone(), entry2.getScheduleExpression().getZone());
        Assert.assertEquals(entry1.getScheduleExpression().getHour(), entry2.getScheduleExpression().getHour());
        Assert.assertEquals(entry1.getScheduleExpression().getMinute(), entry2.getScheduleExpression().getMinute());
        Assert.assertEquals(entry1.getScheduleExpression().getSecond(), entry2.getScheduleExpression().getSecond());
    }
}
