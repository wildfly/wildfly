/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.immutable;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.ee.Immutability;

/**
 * Tests the immutability of {@link Collections} wrappers.
 * N.B. Strictly speaking, an unmodifiable collection is not necessarily immutable since the collection can still be modified through a reference to the delegate collection.
 * Were this the case, the immutability test would also run against the delegate collection - and fail, forcing replication.
 * @author Paul Ferraro
 */
public class CollectionImmutability implements Immutability {

    private final List<Class<?>> unmodifiableCollectionClasses = Arrays.asList(
                Collections.singleton(null).getClass(),
                Collections.singletonList(null).getClass(),
                Collections.unmodifiableCollection(Collections.emptyList()).getClass(),
                Collections.unmodifiableList(Collections.emptyList()).getClass(),
                Collections.unmodifiableNavigableSet(Collections.emptyNavigableSet()).getClass(),
                Collections.unmodifiableSet(Collections.emptySet()).getClass(),
                Collections.unmodifiableSortedSet(Collections.emptySortedSet()).getClass(),
                List.of().getClass(), // ListN
                List.of(Boolean.TRUE).getClass(), // List12
                Set.of().getClass(), // SetN
                Set.of(Boolean.TRUE).getClass()); // Set12

    private final List<Class<?>> unmodifiableMapClasses = Arrays.asList(
                Collections.singletonMap(null, null).getClass(),
                Collections.unmodifiableMap(Collections.emptyMap()).getClass(),
                Collections.unmodifiableNavigableMap(Collections.emptyNavigableMap()).getClass(),
                Collections.unmodifiableSortedMap(Collections.emptySortedMap()).getClass(),
                Map.ofEntries().getClass(), // MapN
                Map.ofEntries(Map.entry(Boolean.TRUE, Boolean.TRUE)).getClass()); // Map1

    private final Immutability elementImmutability;

    public CollectionImmutability(Immutability elementImmutability) {
        this.elementImmutability = elementImmutability;
    }

    @Override
    public boolean test(Object object) {
        for (Class<?> unmodifiableCollectionClass : this.unmodifiableCollectionClasses) {
            if (unmodifiableCollectionClass.isInstance(object)) {
                // An unmodifiable collection is immutable if its members are immutable.
                for (Object element : (Collection<?>) object) {
                    if (!this.elementImmutability.test(element)) return false;
                }
                return true;
            }
        }
        for (Class<?> unmodifiableMapClass : this.unmodifiableMapClasses) {
            if (unmodifiableMapClass.isInstance(object)) {
                // An unmodifiable map is immutable if its entries are immutable.
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
                    if (!this.test(entry)) return false;
                }
                return true;
            }
        }
        if (object instanceof AbstractMap.SimpleImmutableEntry) {
            return this.test((Map.Entry<?, ?>) object);
        }
        return false;
    }

    // An unmodifiable map entry is immutable if its key and value are immutable.
    private boolean test(Map.Entry<?, ?> entry) {
        return this.elementImmutability.test(entry.getKey()) && this.elementImmutability.test(entry.getValue());
    }
}
