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
package org.wildfly.clustering.marshalling.jboss;

import java.io.Externalizable;
import java.io.Serializable;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.TimeZone;

/**
 * {@link org.jboss.marshalling.ClassTable} implementation that dynamically loads {@link ClassTableContributor} instances visible from a given {@link ClassLoader}.
 * @author Paul Ferraro
 */
public class DynamicClassTable extends SimpleClassTable {

    public DynamicClassTable(ClassLoader loader) {
        super(findClasses(loader));
    }

    private static List<Class<?>> findClasses(ClassLoader loader) {
        List<Class<?>> knownClasses = new LinkedList<>();
        for (ClassTableContributor contributor : ServiceLoader.load(ClassTableContributor.class, loader)) {
            knownClasses.addAll(contributor.getKnownClasses());
        }

        List<Class<?>> classes = new ArrayList<>(knownClasses.size() + 30);
        classes.add(Serializable.class);
        classes.add(Externalizable.class);

        // Add common-use non-public JDK implementation classes
        classes.add(Clock.systemDefaultZone().getClass());
        classes.add(TimeZone.getDefault().getClass());
        classes.add(ZoneId.systemDefault().getClass());

        // Add collection wrapper types
        classes.add(Collections.checkedCollection(Collections.emptyList(), Void.class).getClass());
        classes.add(Collections.checkedList(Collections.emptyList(), Void.class).getClass());
        classes.add(Collections.checkedMap(Collections.emptyMap(), Void.class, Void.class).getClass());
        classes.add(Collections.checkedNavigableMap(Collections.emptyNavigableMap(), Void.class, Void.class).getClass());
        classes.add(Collections.checkedNavigableSet(Collections.emptyNavigableSet(), Void.class).getClass());
        classes.add(Collections.checkedQueue(new LinkedList<>(), Void.class).getClass());
        classes.add(Collections.checkedSet(Collections.emptySet(), Void.class).getClass());
        classes.add(Collections.checkedSortedMap(Collections.emptySortedMap(), Void.class, Void.class).getClass());
        classes.add(Collections.checkedSortedSet(Collections.emptySortedSet(), Void.class).getClass());

        classes.add(Collections.synchronizedCollection(Collections.emptyList()).getClass());
        classes.add(Collections.synchronizedList(Collections.emptyList()).getClass());
        classes.add(Collections.synchronizedMap(Collections.emptyMap()).getClass());
        classes.add(Collections.synchronizedNavigableMap(Collections.emptyNavigableMap()).getClass());
        classes.add(Collections.synchronizedNavigableSet(Collections.emptyNavigableSet()).getClass());
        classes.add(Collections.synchronizedSet(Collections.emptySet()).getClass());
        classes.add(Collections.synchronizedSortedMap(Collections.emptySortedMap()).getClass());
        classes.add(Collections.synchronizedSortedSet(Collections.emptySortedSet()).getClass());

        classes.add(Collections.unmodifiableCollection(Collections.emptyList()).getClass());
        classes.add(Collections.unmodifiableList(Collections.emptyList()).getClass());
        classes.add(Collections.unmodifiableMap(Collections.emptyMap()).getClass());
        classes.add(Collections.unmodifiableNavigableMap(Collections.emptyNavigableMap()).getClass());
        classes.add(Collections.unmodifiableNavigableSet(Collections.emptyNavigableSet()).getClass());
        classes.add(Collections.unmodifiableSet(Collections.emptySet()).getClass());
        classes.add(Collections.unmodifiableSortedMap(Collections.emptySortedMap()).getClass());
        classes.add(Collections.unmodifiableSortedSet(Collections.emptySortedSet()).getClass());

        classes.addAll(knownClasses);

        return classes;
    }
}
