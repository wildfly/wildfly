/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Base {@link Externalizer} for boolean-based externalization.
 * @author Paul Ferraro
 */
public class BooleanExternalizer<T> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<Boolean, T> reader;
    private final Function<T, Boolean> writer;

    public BooleanExternalizer(Class<T> targetClass, Function<Boolean, T> reader, Function<T, Boolean> writer) {
        this.targetClass = targetClass;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        output.writeBoolean(this.writer.apply(object).booleanValue());
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.reader.apply(Boolean.valueOf(input.readBoolean()));
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public OptionalInt size(T object) {
        return OptionalInt.of(Byte.BYTES);
    }
}
