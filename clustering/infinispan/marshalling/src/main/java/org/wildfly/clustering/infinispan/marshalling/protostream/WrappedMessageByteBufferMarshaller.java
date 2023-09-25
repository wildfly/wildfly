/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedAction;
import java.util.OptionalInt;
import java.util.ServiceLoader;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link ByteBufferMarshaller} based on a ProtoStream {@link org.infinispan.protostream.WrappedMessage}.
 * @author Paul Ferraro
 */
public class WrappedMessageByteBufferMarshaller implements ByteBufferMarshaller {

    private final ImmutableSerializationContext context;

    public WrappedMessageByteBufferMarshaller(ClassLoader loader) {
        this(createSerializationContext(loader));
    }

    private static ImmutableSerializationContext createSerializationContext(ClassLoader loader) {
        SerializationContext context = ProtobufUtil.newSerializationContext();
        WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Void run() {
                for (SerializationContextInitializer initializer : ServiceLoader.load(SerializationContextInitializer.class, loader)) {
                    initializer.registerSchema(context);
                    initializer.registerMarshallers(context);
                }
                return null;
            }
        });
        return context;
    }

    public WrappedMessageByteBufferMarshaller(ImmutableSerializationContext context) {
        this.context = context;
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.context.canMarshall(object);
    }

    @Override
    public Object readFrom(InputStream input) throws IOException {
        return ProtobufUtil.fromWrappedStream(this.context, input);
    }

    @Override
    public void writeTo(OutputStream output, Object object) throws IOException {
        ProtobufUtil.toWrappedStream(this.context, output, object);
    }

    @Override
    public OptionalInt size(Object object) {
        try {
            return OptionalInt.of(ProtobufUtil.computeWrappedMessageSize(this.context, object));
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }
}
