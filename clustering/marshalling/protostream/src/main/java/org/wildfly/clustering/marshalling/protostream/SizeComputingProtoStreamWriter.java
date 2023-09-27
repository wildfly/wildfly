/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * A {@link ProtoStreamWriter} implementation used to compute the number of bytes that would be written to a stream.
 * @author Paul Ferraro
 */
public class SizeComputingProtoStreamWriter extends AbstractProtoStreamWriter implements Supplier<OptionalInt>, Function<Object, OptionalInt> {

    private final TagWriterImpl writer;
    private final ProtoStreamWriterContext context;
    private boolean present = true;

    public SizeComputingProtoStreamWriter(ProtoStreamSizeOperation operation, ProtoStreamWriterContext context) {
        // Creates a TagWriter using a NoopEncoder
        this(TagWriterImpl.newInstance(operation.getSerializationContext()), context);
    }

    private SizeComputingProtoStreamWriter(TagWriterImpl writer, ProtoStreamWriterContext writerContext) {
        super(writer, writerContext);
        this.writer = writer;
        this.context = writerContext;
    }

    @Override
    public ProtoStreamOperation.Context getContext() {
        return this.context;
    }

    @Override
    public OptionalInt get() {
        return this.present ? OptionalInt.of(this.writer.getWrittenBytes()) : OptionalInt.empty();
    }

    @Override
    public void writeObjectNoTag(Object value) throws IOException {
        OptionalInt size = this.context.computeSize(value, this);
        if (this.present && size.isPresent()) {
            int length = size.getAsInt();
            this.writeVarint32(length);
            if (length > 0) {
                this.writeRawBytes(null, 0, length);
            }
        } else {
            this.present = false;
        }
    }

    @Override
    public OptionalInt apply(Object value) {
        ProtoStreamMarshaller<Object> marshaller = this.findMarshaller(value.getClass());
        return marshaller.size(new DefaultProtoStreamSizeOperation(this.getSerializationContext(), this.context), value);
    }
}
