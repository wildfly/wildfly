/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Used for iterating over an series of iterables, thus avoiding the need to allocate/populate a new list containing all elements.
 * More efficient than the alternative when the number of iterables is arbitrary and small relative to the size of each iterable.
 * @author Paul Ferraro
 */
public class CompositeIterable<T> implements Iterable<T> {

    private final List<? extends Iterable<? extends T>> iterables;

    /**
     * Constructs a new composite iterable.
     * @param iterables a series of iterables
     */
    @SafeVarargs
    public CompositeIterable(Iterable<? extends T>... iterables) {
        this(Arrays.asList(iterables));
    }

    /**
     * Constructs a new composite iterable.
     * @param iterables a series of iterables
     */
    public CompositeIterable(List<? extends Iterable<? extends T>> iterables) {
        this.iterables = iterables;
    }

    @Override
    public Iterator<T> iterator() {
        List<Iterator<? extends T>> iterators = new ArrayList<>(this.iterables.size());
        for (Iterable<? extends T> elements : this.iterables) {
            iterators.add(elements.iterator());
        }
        return new CompositeIterator<>(iterators);
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (Iterable<? extends T> elements : this.iterables) {
            for (T element : elements) {
                result = 31 * result + ((element != null) ? element.hashCode() : 0);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Iterable)) return false;
        @SuppressWarnings("unchecked")
        Iterator<Object> otherElements = ((Iterable<Object>) object).iterator();
        for (Iterable<? extends T> iterable : this.iterables) {
            Iterator<? extends T> elements = iterable.iterator();
            while (elements.hasNext() && otherElements.hasNext()) {
                elements.next().equals(otherElements.next());
            }
            if (elements.hasNext()) return false;
        }
        return !otherElements.hasNext();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        Iterator<? extends Iterable<? extends T>> iterables = this.iterables.iterator();
        while (iterables.hasNext()) {
            Iterator<? extends T> elements = iterables.next().iterator();
            while (elements.hasNext()) {
                if (builder.length() > 1) {
                    builder.append(',').append(' ');
                }
                builder.append(elements.next());
            }
        }
        return builder.append(']').toString();
    }
}
