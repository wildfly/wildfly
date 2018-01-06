/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * {@link Externalizer} based on a {@link Serializer}.
 * @author Paul Ferraro
 */
public class SerializerExternalizer<T> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Serializer<T> serializer;

    public SerializerExternalizer(Class<T> targetClass, Serializer<T> serializer) {
        this.targetClass = targetClass;
        this.serializer = serializer;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        this.serializer.write(output, object);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException {
        return this.serializer.read(input);
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
