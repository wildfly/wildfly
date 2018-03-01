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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tests the immutability of {@link Collections} wrappers.
 * N.B. Strictly speaking, an unmodifiable collection is not necessarily immutable since the collection can still be modified through a reference to the delegate collection.
 * Were this the case, the immutability test would also run against the delegate collection - and fail, forcing replication.
 * @author Paul Ferraro
 */
public class CollectionImmutability implements Predicate<Object> {

    private final List<Class<?>> unmodifiableClasses = Arrays.asList(
                Collections.singleton(null).getClass(),
                Collections.singletonList(null).getClass(),
                Collections.singletonMap(null, null).getClass(),
                Collections.unmodifiableCollection(Collections.emptyList()).getClass(),
                Collections.unmodifiableList(Collections.emptyList()).getClass(),
                Collections.unmodifiableMap(Collections.emptyMap()).getClass(),
                Collections.unmodifiableNavigableMap(Collections.emptyNavigableMap()).getClass(),
                Collections.unmodifiableNavigableSet(Collections.emptyNavigableSet()).getClass(),
                Collections.unmodifiableSet(Collections.emptySet()).getClass(),
                Collections.unmodifiableSortedMap(Collections.emptySortedMap()).getClass(),
                Collections.unmodifiableSortedSet(Collections.emptySortedSet()).getClass());

    private final Predicate<Object> elementImmutability;

    public CollectionImmutability(Predicate<Object> elementImmutability) {
        this.elementImmutability = elementImmutability;
    }

    @Override
    public boolean test(Object object) {
        for (Class<?> unmodifiableClass : this.unmodifiableClasses) {
            if (unmodifiableClass.isInstance(object)) {
                // An unmodifiable set should be immutable.
                if (object instanceof Set) return true;
                // An unmodifiable collection is immutable if its members are immutable.
                // An unmodifiable map should be immutable if its values are immutable.
                Collection<?> collection = (object instanceof Map) ? ((Map<?, ?>) object).values() : (Collection<?>) object;
                // This is not an expensive predicate, so there is little to gain from parallel computation
                for (Object element : collection) {
                    if (!this.elementImmutability.test(element)) return false;
                }
                return true;
            }
        }
        return false;
    }
}
