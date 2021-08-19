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
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * @author Paul Ferraro
 */
public class SizeComputingProtoStreamWriter extends AbstractProtoStreamWriter implements Supplier<OptionalInt> {

    private final TagWriterImpl writer;
    private boolean present = true;

    public SizeComputingProtoStreamWriter(ImmutableSerializationContext context) {
        this(TagWriterImpl.newInstance(context));
    }

    private SizeComputingProtoStreamWriter(TagWriterImpl writer) {
        super(writer);
        this.writer = writer;
    }

    @Override
    public OptionalInt get() {
        return this.present ? OptionalInt.of(this.writer.getWrittenBytes()) : OptionalInt.empty();
    }

    @Override
    public void writeObjectNoTag(Object value) throws IOException {
        if (this.present) {
            ProtoStreamMarshaller<Object> marshaller = this.findMarshaller(value.getClass());
            SizeComputingProtoStreamWriter sizer = new SizeComputingProtoStreamWriter(this.getSerializationContext());
            marshaller.writeTo(sizer, value);
            OptionalInt size = sizer.get();
            if (size.isPresent()) {
                int length = size.getAsInt();
                this.writeVarint32(length);
                this.writeRawBytes(null, 0, length);
            } else {
                this.present = false;
            }
        }
    }
}
