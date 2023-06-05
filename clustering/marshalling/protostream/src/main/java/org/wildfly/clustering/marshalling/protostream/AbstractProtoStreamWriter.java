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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

import org.infinispan.protostream.ProtobufTagMarshaller.WriteContext;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;

/**
 * Delegates most {@link ProtoStreamWriter} operations to a {@link TagWriter}.
 * @author Paul Ferraro
 */
public abstract class AbstractProtoStreamWriter extends AbstractProtoStreamOperation implements ProtoStreamWriter, WriteContext {

    interface ProtoStreamWriterContext extends ProtoStreamOperation.Context {
        /**
         * Returns an existing reference to the specified object, if one exists.
         * @param object an object whose may already have been referenced
         * @return a reference for the specified object, or null, if no reference yet exists.
         */
        Reference getReference(Object object);

        /**
         * Creates a copy of this writer context.
         * Used to ensure reference integrity between size and write operations.
         * @return a copy of this writer context
         */
        ProtoStreamWriterContext clone();

        /**
         * Computes the size of the specified object via the specified function, if not already known
         * @param object the target object
         * @param function a function that computes the size of the specified object
         * @return the computed or cached size
         */
        OptionalInt computeSize(Object object, Function<Object, OptionalInt> function);
    }

    private final TagWriter writer;
    private final int depth;
    private final ProtoStreamWriterContext context;

    protected AbstractProtoStreamWriter(WriteContext context, ProtoStreamWriterContext writerContext) {
        super(context);
        this.writer = context.getWriter();
        this.depth = context.depth();
        this.context = writerContext;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public TagWriter getWriter() {
        return this.writer;
    }

    @Override
    public int depth() {
        return this.depth;
    }

    @Override
    public void writeAnyNoTag(Object value) throws IOException {
        Reference reference = this.context.getReference(value);
        Any any = new Any((reference != null) ? reference : value);
        this.writeObjectNoTag(any);
        if (reference == null) {
            this.context.record(value);
        }
    }

    @Override
    public void writeTag(int number, WireType wireType) throws IOException {
        this.writer.writeTag(number, wireType);
    }

    @Override
    public void writeVarint32(int value) throws IOException {
        this.writer.writeVarint32(value);
    }

    @Override
    public void writeVarint64(long value) throws IOException {
        this.writer.writeVarint64(value);
    }

    @Deprecated
    @Override
    public void writeRawByte(byte value) throws IOException {
        this.writer.writeRawByte(value);
    }

    @Override
    public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
        this.writer.writeRawBytes(value, offset, length);
    }

    @Deprecated
    @Override
    public void writeRawBytes(ByteBuffer value) throws IOException {
        this.writer.writeRawBytes(value);
    }

    @Override
    public void writeBool(int index, boolean value) throws IOException {
        this.writer.writeBool(index, value);
    }

    @Override
    public void writeEnum(int index, int value) throws IOException {
        this.writer.writeEnum(index, value);
    }

    @Deprecated
    @Override
    public void writeInt32(int index, int value) throws IOException {
        this.writer.writeInt32(index, value);
    }

    @Deprecated
    @Override
    public void writeFixed32(int index, int value) throws IOException {
        this.writer.writeFixed32(index, value);
    }

    @Override
    public void writeUInt32(int index, int value) throws IOException {
        this.writer.writeUInt32(index, value);
    }

    @Override
    public void writeSInt32(int index, int value) throws IOException {
        this.writer.writeSInt32(index, value);
    }

    @Override
    public void writeSFixed32(int index, int value) throws IOException {
        this.writer.writeSFixed32(index, value);
    }

    @Deprecated
    @Override
    public void writeInt64(int index, long value) throws IOException {
        this.writer.writeInt64(index, value);
    }

    @Deprecated
    @Override
    public void writeFixed64(int index, long value) throws IOException {
        this.writer.writeFixed64(index, value);
    }

    @Override
    public void writeUInt64(int index, long value) throws IOException {
        this.writer.writeUInt64(index, value);
    }

    @Override
    public void writeSInt64(int index, long value) throws IOException {
        this.writer.writeSInt64(index, value);
    }

    @Override
    public void writeSFixed64(int index, long value) throws IOException {
        this.writer.writeSFixed64(index, value);
    }

    @Override
    public void writeFloat(int index, float value) throws IOException {
        this.writer.writeFloat(index, value);
    }

    @Override
    public void writeDouble(int index, double value) throws IOException {
        this.writer.writeDouble(index, value);
    }

    @Override
    public void writeBytes(int index, byte[] value) throws IOException {
        this.writer.writeBytes(index, value);
    }

    @Override
    public void writeBytes(int index, byte[] value, int offset, int length) throws IOException {
        this.writer.writeBytes(index, value, offset, length);
    }

    @Override
    public void writeBytes(int index, ByteBuffer value) throws IOException {
        this.writer.writeBytes(index, value);
    }

    @Override
    public void writeString(int index, String value) throws IOException {
        this.writer.writeString(index, value);
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }

    static class DefaultProtoStreamWriterContext implements ProtoStreamWriterContext, Function<Object, Reference> {
        private final Map<Object, Reference> references = new IdentityHashMap<>(128);
        private int reference = 0; // Enumerates object references
        private final Map<Object, OptionalInt> sizes;

        DefaultProtoStreamWriterContext() {
            this(new IdentityHashMap<>(128));
        }

        private DefaultProtoStreamWriterContext(Map<Object, OptionalInt> sizes) {
            this.sizes = sizes;
        }

        @Override
        public void record(Object object) {
            if (object != null) {
                this.references.computeIfAbsent(object, this);
            }
        }

        @Override
        public Reference getReference(Object object) {
            return (object != null) ? this.references.get(object) : null;
        }

        @Override
        public Reference apply(Object key) {
            return new Reference(this.reference++);
        }

        @Override
        public ProtoStreamWriterContext clone() {
            // Share size cache
            DefaultProtoStreamWriterContext context = new DefaultProtoStreamWriterContext(this.sizes);
            // Copy references
            context.references.putAll(this.references);
            context.reference = this.reference;
            return context;
        }

        @Override
        public OptionalInt computeSize(Object object, Function<Object, OptionalInt> function) {
            // Don't cache size for internal wrappers, which would otherwise bloat our hashtable
            return (object instanceof Any) ? function.apply(object) : this.sizes.computeIfAbsent(object, function);
        }
    }
}
