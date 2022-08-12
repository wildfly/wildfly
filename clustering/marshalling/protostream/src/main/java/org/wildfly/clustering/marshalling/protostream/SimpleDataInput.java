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

import java.io.DataInput;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * {@link DataInput} implementation used to write the unexposed fields of an {@link org.jgroups.util.Streamable} object.
 * @author Paul Ferraro
 */
public class SimpleDataInput implements DataInput {

    private final Iterator<String> strings;
    private final Iterator<ByteBuffer> buffers;
    private final Iterator<Character> characters;
    private final Iterator<Boolean> booleans;
    private final Iterator<Byte> bytes;
    private final Iterator<Short> shorts;
    private final PrimitiveIterator.OfInt integers;
    private final PrimitiveIterator.OfLong longs;
    private final Iterator<Float> floats;
    private final PrimitiveIterator.OfDouble doubles;

    SimpleDataInput(Builder builder) {
        this.strings = builder.strings.iterator();
        this.buffers = builder.buffers.iterator();
        this.characters = builder.characters.iterator();
        this.booleans = builder.booleans.iterator();
        this.bytes = builder.bytes.iterator();
        this.shorts = builder.shorts.iterator();
        this.integers = builder.integers.iterator();
        this.longs = builder.longs.iterator();
        this.floats = builder.floats.iterator();
        this.doubles = builder.doubles.iterator();
    }

    ByteBuffer nextBuffer() {
        return this.buffers.next();
    }

    @Override
    public String readUTF() {
        return this.strings.next();
    }

    @Override
    public int readInt() {
        return this.integers.nextInt();
    }

    @Override
    public long readLong() {
        return this.longs.nextLong();
    }

    @Override
    public double readDouble() {
        return this.doubles.nextDouble();
    }

    @Override
    public void readFully(byte[] bytes) {
        this.nextBuffer().get(bytes);
    }

    @Override
    public void readFully(byte[] bytes, int offset, int length) {
        this.nextBuffer().get(bytes, offset, length);
    }

    @Override
    public int skipBytes(int n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean readBoolean() {
        return this.booleans.next();
    }

    @Override
    public byte readByte() {
        return this.bytes.next();
    }

    @Override
    public int readUnsignedByte() {
        return Byte.toUnsignedInt(this.bytes.next());
    }

    @Override
    public short readShort() {
        return this.shorts.next();
    }

    @Override
    public int readUnsignedShort() {
        return Short.toUnsignedInt(this.shorts.next());
    }

    @Override
    public char readChar() {
        return this.characters.next();
    }

    @Override
    public float readFloat() {
        return this.floats.next();
    }

    @Override
    public String readLine() {
        return this.strings.next();
    }

    public static class Builder {
        Iterable<String> strings = Collections.emptyList();
        Iterable<ByteBuffer> buffers = Collections.emptyList();
        Iterable<Character> characters = Collections.emptyList();
        Iterable<Boolean> booleans = Collections.emptyList();
        Iterable<Byte> bytes = Collections.emptyList();
        Iterable<Short> shorts = Collections.emptyList();
        IntStream integers = IntStream.empty();
        LongStream longs = LongStream.empty();
        Iterable<Float> floats = Collections.emptyList();
        DoubleStream doubles = DoubleStream.empty();

        public Builder with(String... values) {
            this.strings = Arrays.asList(values);
            return this;
        }

        public Builder with(ByteBuffer... buffers) {
            this.buffers = Arrays.asList(buffers);
            return this;
        }

        public Builder with(char... values) {
            this.characters = new ArrayIterable<>(values);
            return this;
        }

        public Builder with(boolean... values) {
            this.booleans = new ArrayIterable<>(values);
            return this;
        }

        public Builder with(byte... values) {
            this.bytes = new ArrayIterable<>(values);
            return this;
        }

        public Builder with(short... values) {
            this.shorts = new ArrayIterable<>(values);
            return this;
        }

        public Builder with(int... values) {
            this.integers = IntStream.of(values);
            return this;
        }

        public Builder with(long... values) {
            this.longs = LongStream.of(values);
            return this;
        }

        public Builder with(float... values) {
            this.floats = new ArrayIterable<>(values);
            return this;
        }

        public Builder with(double... values) {
            this.doubles = DoubleStream.of(values);
            return this;
        }

        public DataInput build() {
            return new SimpleDataInput(this);
        }
    }

    private static class ArrayIterable<T> implements Iterable<T> {
        private final Object array;

        ArrayIterable(Object array) {
            this.array = array;
        }

        @Override
        public Iterator<T> iterator() {
            Object array = this.array;
            return new Iterator<>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return Array.getLength(array) > this.index;
                }

                @SuppressWarnings("unchecked")
                @Override
                public T next() {
                    return (T) Array.get(array, this.index++);
                }
            };
        }
    }
}
