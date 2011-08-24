/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.pool;

/**
 * Pool that simply creates objects as needed, and destroys them when they are added back into the pool.
 *
 * @author Stuart Douglas
 */
public class InfinitePool<T> implements Pool<T> {

    private final StatelessObjectFactory<T> factory;

    public InfinitePool(final StatelessObjectFactory<T> factory) {
        this.factory = factory;
    }

    @Override
    public void discard(final T obj) {
        factory.destroy(obj);
    }

    @Override
    public T get() {
        return factory.create();
    }

    @Override
    public int getAvailableCount() {
        return 1;
    }

    @Override
    public int getCreateCount() {
        return 0;
    }

    @Override
    public int getCurrentSize() {
        return 0;
    }

    @Override
    public int getMaxSize() {
        return 0;
    }

    @Override
    public int getRemoveCount() {
        return 0;
    }

    @Override
    public void release(final T obj) {
        factory.destroy(obj);
    }

    @Override
    public void setMaxSize(final int maxSize) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
