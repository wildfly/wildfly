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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Supplier;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public class EmptyCollectionExternalizer<T> implements Externalizer<T> {

    private final Supplier<T> factory;

    EmptyCollectionExternalizer(Supplier<T> factory) {
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        // Nothing to write
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.factory.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getTargetClass() {
        return (Class<T>) this.factory.get().getClass();
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptyEnumerationExternalizer extends EmptyCollectionExternalizer<Enumeration<Object>> {
        public EmptyEnumerationExternalizer() {
            super(() -> Collections.emptyEnumeration());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptyIteratorExternalizer extends EmptyCollectionExternalizer<Iterator<Object>> {
        public EmptyIteratorExternalizer() {
            super(() -> Collections.emptyIterator());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptyListExternalizer extends EmptyCollectionExternalizer<List<Object>> {
        public EmptyListExternalizer() {
            super(() -> Collections.emptyList());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptyListIteratorExternalizer extends EmptyCollectionExternalizer<ListIterator<Object>> {
        public EmptyListIteratorExternalizer() {
            super(() -> Collections.emptyListIterator());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptyMapExternalizer extends EmptyCollectionExternalizer<Map<Object, Object>> {
        public EmptyMapExternalizer() {
            super(() -> Collections.emptyMap());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptyNavigableMapExternalizer extends EmptyCollectionExternalizer<NavigableMap<Object, Object>> {
        public EmptyNavigableMapExternalizer() {
            super(() -> Collections.emptyNavigableMap());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptyNavigableSetExternalizer extends EmptyCollectionExternalizer<NavigableSet<Object>> {
        public EmptyNavigableSetExternalizer() {
            super(() -> Collections.emptyNavigableSet());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptySetExternalizer extends EmptyCollectionExternalizer<Set<Object>> {
        public EmptySetExternalizer() {
            super(() -> Collections.emptySet());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptySortedMapExternalizer extends EmptyCollectionExternalizer<SortedMap<Object, Object>> {
        public EmptySortedMapExternalizer() {
            super(() -> Collections.emptySortedMap());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmptySortedSetExternalizer extends EmptyCollectionExternalizer<SortedSet<Object>> {
        public EmptySortedSetExternalizer() {
            super(() -> Collections.emptySortedSet());
        }
    }
}
