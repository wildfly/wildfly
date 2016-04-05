/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan.group;

import org.wildfly.clustering.ejb.infinispan.BeanGroupKey;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupKey<I> implements BeanGroupKey<I> {

    private final I id;

    public InfinispanBeanGroupKey(I id) {
        this.id = id;
    }

    @Override
    public I getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof BeanGroupKey)) return false;
        @SuppressWarnings("unchecked")
        BeanGroupKey<I> key = (BeanGroupKey<I>) object;
        return this.id.equals(key.getId());
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
