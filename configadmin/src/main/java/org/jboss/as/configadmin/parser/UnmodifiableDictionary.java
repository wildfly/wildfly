/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.configadmin.parser;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * An unmodifiable dictionary.
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Dec-2009
 */
class UnmodifiableDictionary<K, V> extends Dictionary<K, V> implements Serializable {

    private static final long serialVersionUID = -6793757957920326746L;

    private final Dictionary<K, V> delegate;

    public UnmodifiableDictionary(Dictionary<K, V> props) {
        if (props == null)
            throw new IllegalArgumentException("Null props");

        delegate = new Hashtable<K, V>();
        Enumeration<K> keys = props.keys();
        while (keys.hasMoreElements()) {
            K key = keys.nextElement();
            V val = props.get(key);
            delegate.put(key, val);
        }
    }

    @Override
    public Enumeration<V> elements() {
        return delegate.elements();
    }

    @Override
    public V get(Object key) {
        return delegate.get(key);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Enumeration<K> keys() {
        return delegate.keys();
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}