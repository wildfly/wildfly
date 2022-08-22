/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
