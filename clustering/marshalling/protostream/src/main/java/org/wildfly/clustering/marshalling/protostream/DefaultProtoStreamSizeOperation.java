/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractProtoStreamWriter.DefaultProtoStreamWriterContext;
import org.wildfly.clustering.marshalling.protostream.AbstractProtoStreamWriter.ProtoStreamWriterContext;
import org.wildfly.common.function.ExceptionBiConsumer;

/**
 * A default ProtoStream size operation.
 * @author Paul Ferraro
 */
public class DefaultProtoStreamSizeOperation extends AbstractProtoStreamOperation implements ProtoStreamSizeOperation {

    private final ProtoStreamWriterContext context;

    /**
     * Creates a new ProtoStream size operation using a new context.
     * @param context the serialization context
     */
    public DefaultProtoStreamSizeOperation(ImmutableSerializationContext context) {
        this(context, new DefaultProtoStreamWriterContext());
    }

    /**
     * Creates a new ProtoStream size operation using the specified context.
     * @param context the serialization context
     * @param sizeContext the context of the size operation
     */
    public DefaultProtoStreamSizeOperation(ImmutableSerializationContext context, ProtoStreamWriterContext writerContext) {
        super(context);
        this.context = writerContext;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public <T> OptionalInt computeSize(ExceptionBiConsumer<ProtoStreamWriter, T, IOException> operation, T value) {
        SizeComputingProtoStreamWriter writer = new SizeComputingProtoStreamWriter(this, this.context);
        try {
            operation.accept(writer, value);
            return writer.get();
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }
}
