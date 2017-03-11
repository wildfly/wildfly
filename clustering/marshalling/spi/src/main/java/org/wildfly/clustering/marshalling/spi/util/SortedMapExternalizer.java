/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;

/**
 * Externalizers for implementations of {@link SortedMap}.
 * Requires additional serialization of the comparator.
 * @author Paul Ferraro
 */
public class SortedMapExternalizer<T extends SortedMap<Object, Object>> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<Comparator<? super Object>, T> factory;

    @SuppressWarnings("unchecked")
    SortedMapExternalizer(Class<?> targetClass, Function<Comparator<? super Object>, T> factory) {
        this.targetClass = (Class<T>) targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T map) throws IOException {
        output.writeObject(map.comparator());
        MapExternalizer.writeMap(output, map);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Comparator<? super Object> comparator = (Comparator<? super Object>) input.readObject();
        int size = IndexExternalizer.VARIABLE.readData(input);
        return MapExternalizer.readMap(input, this.factory.apply(comparator), size);
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @MetaInfServices(Externalizer.class)
    public static class ConcurrentSkipListMapExternalizer extends SortedMapExternalizer<ConcurrentSkipListMap<Object, Object>> {
        public ConcurrentSkipListMapExternalizer() {
            super(ConcurrentSkipListMap.class, ConcurrentSkipListMap<Object, Object>::new);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class TreeMapExternalizer extends SortedMapExternalizer<TreeMap<Object, Object>> {
        public TreeMapExternalizer() {
            super(TreeMap.class, TreeMap<Object, Object>::new);
        }
    }
}
