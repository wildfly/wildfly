/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.catalina.session;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Context;
import org.wildfly.clustering.web.session.SessionContext;

/**
 * Adapts a {@link Context} to a {@link SessionContext}
 * @author Paul Ferraro
 */
public class SessionContextAdapter implements SessionContext {
    private final Context context;

    public SessionContextAdapter(Context context) {
        this.context = context;
    }

    @Override
    public ServletContext getServletContext() {
        return this.context.getServletContext();
    }

    @Override
    public Iterable<HttpSessionAttributeListener> getSessionAttributeListeners() {
        return new FilteredIterable<>(this.context.getApplicationEventListeners(), HttpSessionAttributeListener.class);
    }

    @Override
    public Iterable<HttpSessionListener> getSessionListeners() {
        return new FilteredIterable<>(this.context.getApplicationSessionLifecycleListeners(), HttpSessionListener.class);
    }

    private static class FilteredIterable<T> implements Iterable<T> {
        private final List<?> objects;
        private final Class<T> targetClass;

        FilteredIterable(Object[] objects, Class<T> targetClass) {
            this(Arrays.asList(objects), targetClass);
        }

        FilteredIterable(List<?> objects, Class<T> targetClass) {
            this.objects = objects;
            this.targetClass = targetClass;
        }

        @Override
        public Iterator<T> iterator() {
            return new FilteringIterator<>(this.objects, this.targetClass);
        }
    }

    /*
     * Iterates of a view of a list only containing objects of a specific type.
     */
    private static class FilteringIterator<T> implements Iterator<T> {
        private final Iterator<?> objects;
        private final Class<T> targetClass;
        private volatile T next;

        FilteringIterator(List<?> list, Class<T> targetClass) {
            this.objects = list.iterator();
            this.targetClass = targetClass;
            this.next = this.findNext();
        }

        private T findNext() {
            while (this.objects.hasNext()) {
                Object object = this.objects.next();
                if (this.targetClass.isInstance(object)) {
                    return this.targetClass.cast(object);
                }
            }
            return null;
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public T next() {
            if (this.next == null) throw new NoSuchElementException();
            T next = this.next;
            this.next = this.findNext();
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
