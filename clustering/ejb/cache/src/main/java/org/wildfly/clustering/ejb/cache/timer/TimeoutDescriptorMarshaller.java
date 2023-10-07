/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link TimeoutDescriptor}.
 * @author Paul Ferraro
 */
public enum TimeoutDescriptorMarshaller implements FieldSetMarshaller.Simple<TimeoutDescriptor> {
    INSTANCE;

    private static final int METHOD_NAME_INDEX = 0;
    private static final int PARAMETERS_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public TimeoutDescriptor createInitialValue() {
        return TimeoutDescriptor.DEFAULT;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public TimeoutDescriptor readFrom(ProtoStreamReader reader, int index, WireType type, TimeoutDescriptor descriptor) throws IOException {
        switch (index) {
            case METHOD_NAME_INDEX:
                return new TimeoutDescriptor(reader.readString(), descriptor.getParameters());
            case PARAMETERS_INDEX:
                return new TimeoutDescriptor(descriptor.getMethodName(), reader.readUInt32());
            default:
                reader.skipField(type);
                return descriptor;
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, TimeoutDescriptor descriptor) throws IOException {
        String methodName = descriptor.getMethodName();
        if (!methodName.equals(TimeoutDescriptor.DEFAULT_METHOD_NAME)) {
            writer.writeString(METHOD_NAME_INDEX, methodName);
        }
        int parameters = descriptor.getParameters();
        if (parameters != TimeoutDescriptor.DEFAULT_PARAMETERS) {
            writer.writeUInt32(PARAMETERS_INDEX, parameters);
        }
    }
}
