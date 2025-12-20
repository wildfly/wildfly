/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledValue;

/**
 * @author Paul Ferraro
 */
public class ScheduleTimerMetaDataEntryMarshaller implements ProtoStreamMarshaller<ScheduleTimerMetaDataEntry<Object>> {

    private static final int INFO_INDEX = 1;
    private static final int START_INDEX = 2;
    private static final int LAST_TIMEOUT_INDEX = 3;
    private static final int TIMEOUT_DESCRIPTOR_INDEX = 4;
    private static final int SCHEDULE_EXPRESSION_INDEX = 5;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ScheduleTimerMetaDataEntry<Object>> getJavaClass() {
        return (Class<ScheduleTimerMetaDataEntry<Object>>) (Class<?>) ScheduleTimerMetaDataEntry.class;
    }

    @Override
    public ScheduleTimerMetaDataEntry<Object> readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<ImmutableScheduleExpressionBuilder> expressionReader = reader.createFieldSetReader(ImmutableScheduleExpressionMarshaller.INSTANCE, SCHEDULE_EXPRESSION_INDEX);
        ByteBufferMarshalledValue<Object> context = null;
        Instant start = Instant.EPOCH;
        Duration lastTimeout = null;
        ImmutableScheduleExpressionBuilder expressionBuilder = ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue();
        Predicate<Method> timeoutMatcher = DefaultTimeoutMatcher.INSTANCE;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            switch (index) {
                case INFO_INDEX:
                    context = reader.readObject(ByteBufferMarshalledValue.class);
                    break;
                case START_INDEX:
                    start = reader.readObject(Instant.class);
                    break;
                case LAST_TIMEOUT_INDEX:
                    lastTimeout = reader.readObject(Duration.class);
                    break;
                case TIMEOUT_DESCRIPTOR_INDEX:
                    timeoutMatcher = reader.readObject(TimeoutDescriptor.class);
                    break;
                default:
                    if (expressionReader.contains(index)) {
                        expressionBuilder = expressionReader.readField(expressionBuilder);
                    } else {
                        reader.skipField(tag);
                    }
            }
        }
        ScheduleTimerMetaDataEntry<Object> entry = new ScheduleTimerMetaDataEntry<>(context, start, expressionBuilder.get(), timeoutMatcher);
        entry.setLastTimeout(lastTimeout);
        return entry;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ScheduleTimerMetaDataEntry<Object> entry) throws IOException {
        Object context = entry.getContext();
        if (context != null) {
            writer.writeObject(INFO_INDEX, context);
        }
        Instant start = entry.getStart();
        if (!start.equals(Instant.EPOCH)) {
            writer.writeObject(START_INDEX, start);
        }
        Duration lastTimeout = entry.getLastTimeout();
        if (lastTimeout != null) {
            writer.writeObject(LAST_TIMEOUT_INDEX, lastTimeout);
        }
        Predicate<Method> matcher = entry.getTimeoutMatcher();
        if (matcher != DefaultTimeoutMatcher.INSTANCE) {
            writer.writeObject(TIMEOUT_DESCRIPTOR_INDEX, matcher);
        }
        writer.createFieldSetWriter(ImmutableScheduleExpressionMarshaller.INSTANCE, SCHEDULE_EXPRESSION_INDEX).writeFields(entry.getScheduleExpression());
    }
}
