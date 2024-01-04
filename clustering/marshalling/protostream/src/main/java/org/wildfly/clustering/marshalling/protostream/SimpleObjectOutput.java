/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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