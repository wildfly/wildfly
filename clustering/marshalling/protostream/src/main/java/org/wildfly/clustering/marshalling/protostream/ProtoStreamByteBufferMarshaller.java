/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class ProtoStreamByteBufferMarshaller implements ByteBufferMarshaller {

    private final ImmutableSerializationContext context;
    private final Predictable<Any> marshaller;

    public ProtoStreamByteBufferMarshaller(ImmutableSerializationContext context) {
        this.context = context;
        this.marshaller = (ProtoStreamMarshaller<Any>) context.getMarshaller(Any.class);
    }

    @Override
    public OptionalInt size(Object value) {
        return this.marshaller.size(this.context, new Any(value));
    }

    @Override
    public boolean isMarshallable(Object object) {
        if ((object == null) || (object instanceof Class)) return true;
        Class<?> targetClass = object.getClass();
        if ((object instanceof Throwable) && (WildFlySecurityManager.getClassLoaderPrivileged(targetClass) == WildFlySecurityManager.getClassLoaderPrivileged(Throwable.class))) return true;
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
        return this.context.canMarshall(targetClass);
    }

    @Override
    public Object readFrom(InputStream input) throws IOException {
        try (ProtoStreamReaderContext context = ProtoStreamReaderContext.INSTANCE.get()) {
            return ProtobufUtil.readFrom(this.context, input, Any.class).get();
        }
    }

    @Override
    public void writeTo(OutputStream output, Object object) throws IOException {
        try (ProtoStreamWriterContext context = ProtoStreamWriterContext.INSTANCE.get()) {
            ProtobufUtil.writeTo(this.context, output, new Any(object));
        }
    }
}
