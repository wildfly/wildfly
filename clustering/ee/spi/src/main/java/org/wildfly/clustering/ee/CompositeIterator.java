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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator that iterates over a series of iterators.
 * @author Paul Ferraro
 */
public class CompositeIterator<E> implements Iterator<E> {

    private final Iterable<? extends Iterator<? extends E>> iterators;

    /**
     * Constructs a new composite iterator.
     * @param iterables a series of iterators
     */
    @SafeVarargs
    public CompositeIterator(Iterator<? extends E>... iterators) {
        this(Arrays.asList(iterators));
    }

    /**
     * Constructs a new composite iterator.
     * @param iterables a series of iterators
     */
    public CompositeIterator(Iterable<? extends Iterator<? extends E>> iterators) {
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        for (Iterator<? extends E> iterator : this.iterators) {
            if (iterator.hasNext()) return true;
        }
        return false;
    }

    @Override
    public E next() {
        for (Iterator<? extends E> iterator : this.iterators) {
            if (iterator.hasNext()) return iterator.next();
        }
        throw new NoSuchElementException();
    }
}
