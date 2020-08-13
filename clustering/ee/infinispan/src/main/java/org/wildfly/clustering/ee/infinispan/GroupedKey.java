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

package org.wildfly.clustering.ee.infinispan;

import java.util.Objects;

import org.infinispan.distribution.group.Group;
import org.wildfly.clustering.ee.cache.Key;

/**
 * A cache key supporting group co-location.
 * @author Paul Ferraro
 */
public class GroupedKey<K> implements Key<K> {
    private final K id;

    public GroupedKey(K id) {
        this.id = id;
    }

    /**
     * Returns the value of this key.
     * @return the key value
     */
    @Override
    public K getId() {
        return this.id;
    }

    @Group
    public String getGroup() {
        return this.id.toString();
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || (object.getClass() != this.getClass())) return false;
        @SuppressWarnings("unchecked")
        GroupedKey<K> key = (GroupedKey<K>) object;
        return this.id.equals(key.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), this.id);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.getClass().getSimpleName(), this.id.toString());
    }
}
