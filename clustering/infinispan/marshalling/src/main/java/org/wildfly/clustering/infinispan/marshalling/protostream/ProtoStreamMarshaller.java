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

package org.wildfly.clustering.infinispan.marshalling.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.jboss.modules.Module;
import org.wildfly.clustering.infinispan.marshalling.AbstractMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;

/**
 * @author Paul Ferraro
 */
public class ProtoStreamMarshaller extends AbstractMarshaller {

    private final ProtoStreamByteBufferMarshaller marshaller;

    public ProtoStreamMarshaller(Module module) {
        this(module.getClassLoader());
    }

    public ProtoStreamMarshaller(ClassLoader loader) {
        this(new SerializationContextBuilder().register(loader));
    }

    protected ProtoStreamMarshaller(SerializationContextBuilder builder) {
        this(builder.register(new IOSerializationContextInitializer()).build());
    }

    public ProtoStreamMarshaller(ImmutableSerializationContext context) {
        this.marshaller = new ProtoStreamByteBufferMarshaller(context);
    }

    @Override
    public int sizeEstimate(Object object) {
        return this.marshaller.size(object).orElse(512);
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.marshaller.isMarshallable(object);
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_PROTOSTREAM;
    }

    @Override
    public void writeObject(Object object, OutputStream output) throws IOException {
        this.marshaller.writeTo(output, object);
    }

    @Override
    public Object readObject(InputStream input) throws IOException {
        return this.marshaller.readFrom(input);
    }
}
