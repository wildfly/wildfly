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
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamDataInput;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamDataOutput;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ScalarMarshaller;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.web.IdentifierSerializerProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Scalar marshaller for a session identifier.
 * @author Paul Ferraro
 */
public enum SessionIdentifierMarshaller implements ScalarMarshaller<String> {
    INSTANCE;

    private final Serializer<String> serializer = loadSerializer();

    private static Serializer<String> loadSerializer() {
        Iterator<IdentifierSerializerProvider> providers = load(IdentifierSerializerProvider.class).iterator();
        if (!providers.hasNext()) {
            throw new ServiceConfigurationError(IdentifierSerializerProvider.class.getName());
        }
        return providers.next().getSerializer();
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
        return this.serializer.read(new ProtoStreamDataInput(reader));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, String id) throws IOException {
        this.serializer.write(new ProtoStreamDataOutput(writer), id);
    }

    @Override
    public Class<? extends String> getJavaClass() {
        return String.class;
    }

    @Override
    public int getWireType() {
        return WireFormat.WIRETYPE_LENGTH_DELIMITED;
    }
}
