/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Generic {@link Externalizer} for an object composed of 2 externalizable components.
 * @author Paul Ferraro
 */
public class BinaryExternalizer<T, X, Y> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Externalizer<X> externalizer1;
    private final Externalizer<Y> externalizer2;
    private final Function<T, X> accessor1;
    private final Function<T, Y> accessor2;
    private final BiFunction<X, Y, T> factory;

    public BinaryExternalizer(Class<T> targetClass, Externalizer<X> externalizer1, Externalizer<Y> externalizer2, Function<T, X> accessor1, Function<T, Y> accessor2, BiFunction<X, Y, T> factory) {
        this.targetClass = targetClass;
        this.externalizer1 = externalizer1;
        this.externalizer2 = externalizer2;
        this.accessor1 = accessor1;
        this.accessor2 = accessor2;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        this.externalizer1.writeObject(output, this.accessor1.apply(object));
        this.externalizer2.writeObject(output, this.accessor2.apply(object));
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.factory.apply(this.externalizer1.readObject(input), this.externalizer2.readObject(input));
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
