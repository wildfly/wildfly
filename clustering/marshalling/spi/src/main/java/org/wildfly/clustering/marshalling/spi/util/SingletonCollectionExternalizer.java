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

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public class SingletonCollectionExternalizer<T extends Collection<Object>> implements Externalizer<T> {

    private final Function<Object, T> factory;

    SingletonCollectionExternalizer(Function<Object, T> factory) {
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T collection) throws IOException {
        output.writeObject(collection.stream().findFirst().get());
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.factory.apply(input.readObject());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends T> getTargetClass() {
        return (Class<? extends T>) this.factory.apply(null).getClass();
    }

    @MetaInfServices(Externalizer.class)
    public static class SingletonListExternalizer extends SingletonCollectionExternalizer<List<Object>> {
        public SingletonListExternalizer() {
            super(Collections::singletonList);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class SingletonSetExternalizer extends SingletonCollectionExternalizer<Set<Object>> {
        public SingletonSetExternalizer() {
            super(Collections::singleton);
        }
    }
}
