/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.io.InvalidClassException;
import java.io.InvalidObjectException;

/**
 * Marshaller that stores attribute values using marshalled values.
 * @author Paul Ferraro
 */
public class MarshalledValueMarshaller<V, C extends Marshallability> implements Marshaller<V, MarshalledValue<V, C>> {
    private final MarshalledValueFactory<C> factory;
    private final C context;

    public MarshalledValueMarshaller(MarshalledValueFactory<C> factory, C context) {
        this.factory = factory;
        this.context = context;
    }

    @Override
    public V read(MarshalledValue<V, C> value) throws InvalidSerializedFormException {
        if (value == null) return null;
        try {
            return value.get(this.context);
        } catch (ClassNotFoundException | InvalidClassException | InvalidObjectException e) {
            throw new InvalidSerializedFormException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public MarshalledValue<V, C> write(V object) {
        if (object == null) return null;
        return this.factory.createMarshalledValue(object);
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.context.isMarshallable(object);
    }
}
