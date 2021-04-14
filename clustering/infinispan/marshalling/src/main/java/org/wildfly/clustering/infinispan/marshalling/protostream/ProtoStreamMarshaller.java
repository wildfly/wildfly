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

import java.util.function.UnaryOperator;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.wildfly.clustering.infinispan.marshalling.AbstractUserMarshaller;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;

/**
 * @author Paul Ferraro
 */
public class ProtoStreamMarshaller extends AbstractUserMarshaller {

    public ProtoStreamMarshaller(ClassLoaderMarshaller loaderMarshaller, UnaryOperator<SerializationContextBuilder> builder) {
        this(builder.apply(new SerializationContextBuilder(loaderMarshaller).register(new IOSerializationContextInitializer())).build());
    }

    public ProtoStreamMarshaller(ImmutableSerializationContext context) {
        super(new ProtoStreamByteBufferMarshaller(context));
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_PROTOSTREAM;
    }
}
