/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller.ReadContext;
import org.infinispan.protostream.ProtobufTagMarshaller.WriteContext;
import org.infinispan.protostream.impl.TagReaderImpl;
import org.infinispan.protostream.impl.TagWriterImpl;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 */
public class ProtoStreamByteBufferMarshaller implements ByteBufferMarshaller {

    private final ImmutableSerializationContext context;

    public ProtoStreamByteBufferMarshaller(ImmutableSerializationContext context) {
        this.context = context;
    }

    @Override
    public OptionalInt size(Object object) {
        ProtoStreamSizeOperation operation = new DefaultProtoStreamSizeOperation(this.context);
        ProtoStreamMarshaller<Any> marshaller = operation.findMarshaller(Any.class);
        return marshaller.size(operation, new Any(object));
    }

    @Override
    public boolean isMarshallable(Object object) {
        if ((object == null) || (object instanceof Class)) return true;
        Class<?> targetClass = object.getClass();
        if (AnyField.fromJavaType(targetClass) != null) return true;
        if (targetClass.isArray()) {
            for (int i = 0; i < Array.getLength(object); ++i) {
                if (!this.isMarshallable(Array.get(object, i))) return false;
            }
            return true;
        }
        if (Proxy.isProxyClass(targetClass)) {
            return this.isMarshallable(Proxy.getInvocationHandler(object));
        }
        while (targetClass != null) {
            if (this.context.canMarshall(targetClass)) {
                return true;
            }
            targetClass = targetClass.getSuperclass();
        }
        return false;
    }

    @Override
    public Object readFrom(InputStream input) throws IOException {
        ReadContext context = TagReaderImpl.newInstance(this.context, input);
        ProtoStreamReader reader = new DefaultProtoStreamReader(context);
        ProtoStreamMarshaller<Any> marshaller = reader.findMarshaller(Any.class);
        return marshaller.readFrom(reader).get();
    }

    @Override
    public void writeTo(OutputStream output, Object object) throws IOException {
        WriteContext context = TagWriterImpl.newInstanceNoBuffer(this.context, output);
        ProtoStreamWriter writer = new DefaultProtoStreamWriter(context);
        ProtoStreamMarshaller<Any> marshaller = writer.findMarshaller(Any.class);
        marshaller.writeTo(writer, new Any(object));
    }
}
