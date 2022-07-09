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

import java.io.ObjectOutput;
import java.util.function.Consumer;

import org.wildfly.common.function.Functions;

/**
 * {@link ObjectOutput} implementation used to read the unexposed fields of an {@link java.io.Externalizable} object.
 * @author Paul Ferraro
 */
public class SimpleObjectOutput extends SimpleDataOutput implements ObjectOutput {

    private final Consumer<Object> objects;

    SimpleObjectOutput(Builder builder) {
        super(builder);
        this.objects = builder.objects;
    }

    @Override
    public void writeObject(Object value) {
        this.objects.accept(value);
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    public static class Builder extends SimpleDataOutput.Builder {
        Consumer<Object> objects = Functions.discardingConsumer();

        public Builder with(Object[] values) {
            this.objects = new SimpleDataOutput.ArrayConsumer<>(values);
            return this;
        }

        @Override
        public Builder with(String[] values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(char[] values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(boolean[] values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(byte[] values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(short[] values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(int[] values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(long[] values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(float[] values) {
            super.with(values);
            return this;
        }

        @Override
        public Builder with(double[] values) {
            super.with(values);
            return this;
        }

        @Override
        public ObjectOutput build() {
            return new SimpleObjectOutput(this);
        }
    }
}