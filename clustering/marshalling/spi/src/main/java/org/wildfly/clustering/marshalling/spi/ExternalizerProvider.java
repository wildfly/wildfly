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
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public interface ExternalizerProvider extends Externalizer<Object> {

    Externalizer<?> getExternalizer();

    @Override
    default void writeObject(ObjectOutput output, Object object) throws IOException {
        this.cast(Object.class).writeObject(output, object);
    }

    @Override
    default Object readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.getExternalizer().readObject(input);
    }

    @Override
    default Class<Object> getTargetClass() {
        return this.cast(Object.class).getTargetClass();
    }

    @Override
    default OptionalInt size(Object object) {
        return this.cast(Object.class).size(object);
    }

    @SuppressWarnings("unchecked")
    default <T> Externalizer<T> cast(Class<T> type) {
        if (!type.isAssignableFrom(this.getExternalizer().getTargetClass())) {
            throw new IllegalArgumentException(type.getName());
        }
        return (Externalizer<T>) this.getExternalizer();
    }
}
