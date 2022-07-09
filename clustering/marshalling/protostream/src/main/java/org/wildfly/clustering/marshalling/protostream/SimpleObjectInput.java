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

import java.io.ObjectInput;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * {@link ObjectInput} implementation used to write the unexposed fields of an {@link java.io.Externalizable} object.
 * @author Paul Ferraro
 */
public class SimpleObjectInput extends SimpleDataInput implements ObjectInput {

    private final Iterator<Object> objects;

    SimpleObjectInput(Builder builder) {
        super(builder);
        this.objects = builder.objects.iterator();
    }

    @Override
    public Object readObject() {
        return this.objects.next();
    }

    @Override
    public int read() {
        return this.readByte();
    }

    @Override
    public int read(byte[] bytes) {
        ByteBuffer buffer = this.nextBuffer();
        int start = buffer.position();
        return buffer.get(bytes).position() - start;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
        ByteBuffer buffer = this.nextBuffer();
        int start = buffer.position();
        return buffer.get(bytes, offset, length).position() - start;
    }

    @Override
    public long skip(long n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int available() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        // Nothing to close
    }

    public static class Builder extends SimpleDataInput.Builder {
        List<Object> objects = Collections.emptyList();

        public Builder with(Object... values) {
            this.objects = Arrays.asList(values);
            return this;
        }

        @Override
        public Builder with(String... values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(ByteBuffer... buffers) {
            super.with(buffers);
            return this;
        }

        @Override
        public Builder with(char... values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(boolean... values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(byte... values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(short... values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(int... values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(long... values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(float... values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(double... values) {
            super.with(values);
            return this;
        }

        @Override
        public ObjectInput build() {
            return new SimpleObjectInput(this);
        }
    }
}
