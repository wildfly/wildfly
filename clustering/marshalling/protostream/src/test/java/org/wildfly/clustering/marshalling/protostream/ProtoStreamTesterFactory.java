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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferTestMarshaller;

/**
 * @author Paul Ferraro
 */
public class ProtoStreamTesterFactory implements MarshallingTesterFactory {

    private final ImmutableSerializationContext context;

    public ProtoStreamTesterFactory() {
        this.context = new SerializationContextBuilder().build();
    }

    public ProtoStreamTesterFactory(ClassLoader loader) {
        this.context = new SerializationContextBuilder().register(loader).build();
    }

    @Override
    public <T> MarshallingTester<T> createTester() {
        return new MarshallingTester<>(new ByteBufferTestMarshaller<>(new ProtoStreamByteBufferMarshaller(this.context)));
    }
}
