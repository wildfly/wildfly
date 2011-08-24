/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.ejb3.pool.infinite;

import org.jboss.ejb3.pool.AbstractPool;
import org.jboss.ejb3.pool.StatelessObjectFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * A pool that has no constraints.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
public class InfinitePool<T> extends AbstractPool<T> {
    private List<T> active = new LinkedList<T>();

    /**
     * Needs to be part of the sync block, because else other threads will miss it.
     */
    private int size;

    public InfinitePool(StatelessObjectFactory<T> factory) {
        super(factory);
    }

    @Override
    public void discard(T obj) {
        throw new RuntimeException("NYI");
    }

    @Override
    public T get() {
        T obj = create();
        synchronized (active) {
            active.add(obj);
            size = active.size();
        }
        return obj;
    }

    @Override
    public int getAvailableCount() {
        return -1;
    }

    @Override
    public int getCurrentSize() {
        return size;
    }

    @Override
    public int getMaxSize() {
        return -1;
    }

    @Override
    public void release(T obj) {
        synchronized (active) {
            boolean contains = active.remove(obj);
            if (!contains)
                throw new IllegalArgumentException(obj + " is not of this pool");
        }
        destroy(obj);
    }

    @Override
    public void setMaxSize(int maxSize) {
        // makes no sense on an infinite pool
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO: this is a bit wicked
        for (T obj : active) {
            destroy(obj);
        }
        active.clear();
        size = 0;
    }

}
