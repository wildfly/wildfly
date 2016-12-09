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

package org.wildfly.clustering.ee;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests the immutability of {@link Collections} wrappers.
 * N.B. Strictly speaking, an unmodifiable collection is not necessarily immutable since the collection can still be modified through a reference to the delegate collection.
 * Were this the case, the immutability test would also run against the delegate collection - and fail, forcing replication.
 * @author Paul Ferraro
 */
public class CollectionImmutability implements Predicate<Object> {

    private final Set<Class<?>> unmodifiableClasses = Immutability.createIdentitySet(Stream.of(
            Collections.singleton(null),
            Collections.singletonList(null),
            Collections.singletonMap(null, null),
            Collections.unmodifiableCollection(Collections.emptyList()),
            Collections.unmodifiableList(Collections.emptyList()),
            Collections.unmodifiableMap(Collections.emptyMap()),
            Collections.unmodifiableNavigableMap(Collections.emptyNavigableMap()),
            Collections.unmodifiableNavigableSet(Collections.emptyNavigableSet()),
            Collections.unmodifiableSet(Collections.emptySet()),
            Collections.unmodifiableSortedMap(Collections.emptySortedMap()),
            Collections.unmodifiableSortedSet(Collections.emptySortedSet())
        ).map(o -> o.getClass()).collect(Collectors.toList()));

    private final Predicate<Object> elementImmutability;

    public CollectionImmutability(Predicate<Object> elementImmutability) {
        this.elementImmutability = elementImmutability;
    }

    @Override
    public boolean test(Object object) {
        if (this.unmodifiableClasses.stream().anyMatch(immutableClass -> immutableClass.isInstance(object))) {
            // An unmodifiable set should be immutable.
            if (object instanceof Set) return true;
            // An unmodifiable collection is immutable if its members are immutable.
            // An unmodifiable map should be immutable if its values are immutable.
            Collection<?> collection = (object instanceof Map) ? ((Map<?, ?>) object).values() : (Collection<?>) object;
            return collection.stream().allMatch(e -> this.elementImmutability.test(e));
        }
        return false;
    }
}
