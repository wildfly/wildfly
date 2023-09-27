/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * @author Paul Ferraro
 */
public class ScheduleTimerCreationMetaDataMarshaller implements ProtoStreamMarshaller<ScheduleTimerCreationMetaData<Object>> {

    private static final int INFO_INDEX = 1;
    private static final int START_INDEX = 2;
    private static final int SCHEDULE_EXPRESSION_INDEX = 3;
    private static final int NO_PARAMETERS_METHOD_NAME_INDEX = SCHEDULE_EXPRESSION_INDEX + ImmutableScheduleExpressionMarshaller.INSTANCE.getFields();
    private static final int TIMER_PARAMETERS_METHOD_NAME_INDEX = NO_PARAMETERS_METHOD_NAME_INDEX + 1;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ScheduleTimerCreationMetaData<Object>> getJavaClass() {
        return (Class<ScheduleTimerCreationMetaDataEntry<Object>>) (Class<?>) ScheduleTimerCreationMetaDataEntry.class;
    }

    @Override
    public ScheduleTimerCreationMetaData<Object> readFrom(ProtoStreamReader reader) throws IOException {
        Instant creation = Instant.EPOCH;
        MarshalledValue<Object, Object> context = null;
        ImmutableScheduleExpressionBuilder expressionBuilder = ImmutableScheduleExpressionMarshaller.INSTANCE.getBuilder();
        TimeoutDescriptor descriptor = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index == START_INDEX) {
                creation = reader.readObject(Instant.class);
            } else if (index == INFO_INDEX) {
                context = reader.readObject(ByteBufferMarshalledValue.class);
            } else if ((index >= SCHEDULE_EXPRESSION_INDEX) && (index < NO_PARAMETERS_METHOD_NAME_INDEX)) {
                expressionBuilder = ImmutableScheduleExpressionMarshaller.INSTANCE.readField(reader, index - SCHEDULE_EXPRESSION_INDEX, expressionBuilder);
            } else if (index == NO_PARAMETERS_METHOD_NAME_INDEX) {
                descriptor = new TimeoutDescriptor(reader.readString(), 0);
            } else if (index == TIMER_PARAMETERS_METHOD_NAME_INDEX) {
                descriptor = new TimeoutDescriptor(reader.readString(), 1);
            } else {
                reader.skipField(tag);
            }
        }
        return new ScheduleTimerCreationMetaDataEntry<>(context, creation, expressionBuilder.build(), descriptor);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ScheduleTimerCreationMetaData<Object> metaData) throws IOException {
        Object context = metaData.getContext();
        if (context != null) {
            writer.writeObject(INFO_INDEX, context);
        }
        Instant start = metaData.getStart();
        if (!start.equals(Instant.EPOCH)) {
            writer.writeObject(START_INDEX, start);
        }
        ImmutableScheduleExpressionMarshaller.INSTANCE.writeFields(writer, SCHEDULE_EXPRESSION_INDEX, metaData.getScheduleExpression());
        TimeoutDescriptor descriptor = metaData.getTimeoutMatcher();
        if (descriptor != null) {
            writer.writeString(descriptor.getParameters() > 0 ? TIMER_PARAMETERS_METHOD_NAME_INDEX : NO_PARAMETERS_METHOD_NAME_INDEX, descriptor.getMethodName());
        }
    }
}
