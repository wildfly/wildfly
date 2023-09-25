/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
