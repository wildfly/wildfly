/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan;

import java.util.Map;

import org.infinispan.util.function.SerializablePredicate;

/**
 * Filters a cache for entries specific to a particular bean.
 * @author Paul Ferraro
 */
public class BeanFilter<I> implements SerializablePredicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> {
    private static final long serialVersionUID = -1079989480899595045L;

    private final String beanName;

    public BeanFilter(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public boolean test(Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>> entry) {
        if (entry.getKey() instanceof BeanKey) {
            Object value = entry.getValue();
            if (value instanceof BeanEntry) {
                return this.beanName.equals(((BeanEntry<?>) value).getBeanName());
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.beanName;
    }
}
