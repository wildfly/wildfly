/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class TimerIndexMarshaller implements ProtoStreamMarshaller<TimerIndex> {

    private static final int CLASS_NAME_INDEX = 1;
    private static final int TIMEOUT_DESCRIPTOR_INDEX = 2;
    private static final int INDEX_INDEX = TimeoutDescriptorMarshaller.INSTANCE.nextIndex(TIMEOUT_DESCRIPTOR_INDEX);

    private static final int DEFAULT_INDEX = 0;

    @Override
    public Class<? extends TimerIndex> getJavaClass() {
        return TimerIndex.class;
    }

    @Override
    public TimerIndex readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<TimeoutDescriptor> descriptorReader = reader.createFieldSetReader(TimeoutDescriptorMarshaller.INSTANCE, TIMEOUT_DESCRIPTOR_INDEX);
        String declaringClassName = null;
        TimeoutDescriptor descriptor = TimeoutDescriptorMarshaller.INSTANCE.createInitialValue();
        int timerIndex = DEFAULT_INDEX;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            switch (index) {
                case CLASS_NAME_INDEX:
                    declaringClassName = reader.readString();
                    break;
                default:
                    if (descriptorReader.contains(index)) {
                        descriptor = descriptorReader.readField(descriptor);
                    } else if (index == INDEX_INDEX) {
                        timerIndex = reader.readUInt32();
                    } else {
                        reader.skipField(tag);
                    }
            }
        }
        return new TimerIndex(declaringClassName, descriptor.getMethodName(), descriptor.getParameters(), timerIndex);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, TimerIndex index) throws IOException {
        String className = index.getDeclaringClassName();
        if (className != null) {
            writer.writeString(CLASS_NAME_INDEX, className);
        }
        writer.createFieldSetWriter(TimeoutDescriptorMarshaller.INSTANCE, TIMEOUT_DESCRIPTOR_INDEX).writeFields(index);
        if (index.getIndex() != DEFAULT_INDEX) {
            writer.writeUInt32(INDEX_INDEX, index.getIndex());
        }
    }
}
