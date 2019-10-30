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

package org.wildfly.clustering.infinispan.spi.distribution;

import java.util.Objects;

import org.infinispan.distribution.group.Group;

/**
 * A cache key supporting group co-location.
 * @author Paul Ferraro
 */
public class Key<K> {
    private final K value;

    public Key(K value) {
        this.value = value;
    }

    /**
     * Returns the value of this key.
     * @return the key value
     */
    public K getValue() {
        return this.value;
    }

    @Group
    public String getGroup() {
        return this.value.toString();
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || (object.getClass() != this.getClass())) return false;
        @SuppressWarnings("unchecked")
        Key<K> key = (Key<K>) object;
        return this.value.equals(key.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), this.value);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.getClass().getSimpleName(), this.value.toString());
    }
}
