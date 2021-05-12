/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ScalarMarshaller;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.IdentifierMarshallerProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Scalar marshaller for a session identifier.
 * @author Paul Ferraro
 */
public enum SessionIdentifierMarshaller implements ScalarMarshaller<String> {
    INSTANCE;

    private final Marshaller<String, ByteBuffer> marshaller = loadMarshaller();

    private static Marshaller<String, ByteBuffer> loadMarshaller() {
        Iterator<IdentifierMarshallerProvider> providers = load(IdentifierMarshallerProvider.class).iterator();
        if (!providers.hasNext()) {
            throw new ServiceConfigurationError(IdentifierMarshallerProvider.class.getName());
        }
        return providers.next().getMarshaller();
    }

    private static <T> Iterable<T> load(Class<T> providerClass) {
        PrivilegedAction<Iterable<T>> action = new PrivilegedAction<Iterable<T>>() {
            @Override
            public Iterable<T> run() {
                return ServiceLoader.load(providerClass, providerClass.getClassLoader());
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    }

    @Override
    public String readFrom(ProtoStreamReader reader) throws IOException {
        return this.marshaller.read(reader.readByteBuffer());
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, String id) throws IOException {
        ByteBuffer buffer = this.marshaller.write(id);
        int offset = buffer.arrayOffset();
        int length = buffer.limit() - offset;
        writer.writeVarint32(length);
        writer.writeRawBytes(buffer.array(), offset, length);
    }

    @Override
    public Class<? extends String> getJavaClass() {
        return String.class;
    }

    @Override
    public WireType getWireType() {
        return WireType.LENGTH_DELIMITED;
    }
}
