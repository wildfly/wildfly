/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AttachmentList<T> implements List<T>, RandomAccess {

    private final Class<T> valueClass;
    private final List<T> delegate;

    public AttachmentList(final int initialCapacity, final Class<T> valueClass) {
        delegate = Collections.checkedList(new ArrayList<T>(initialCapacity), valueClass);
        this.valueClass = valueClass;
    }

    public AttachmentList(final Class<T> valueClass) {
        delegate = Collections.checkedList(new ArrayList<T>(), valueClass);
        this.valueClass = valueClass;
    }

    public AttachmentList(final Collection<? extends T> c, final Class<T> valueClass) {
        delegate = Collections.checkedList(new ArrayList<T>(c.size()), valueClass);
        delegate.addAll(c);
        this.valueClass = valueClass;
    }

    public Class<T> getValueClass() {
        return valueClass;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <A> A[] toArray(final A[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(final T t) {
        return delegate.add(t);
    }

    @Override
    public boolean remove(final Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        return delegate.addAll(index, c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public T get(final int index) {
        return delegate.get(index);
    }

    @Override
    public T set(final int index, final T element) {
        return delegate.set(index, element);
    }

    @Override
    public void add(final int index, final T element) {
        delegate.add(index, element);
    }

    @Override
    public T remove(final int index) {
        return delegate.remove(index);
    }

    @Override
    public int indexOf(final Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(final Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        return delegate.listIterator(index);
    }

    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }

    @Override
    public boolean equals(final Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return "AttachmentList" + delegate;
    }
}
