/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.infinispan.bean;

import java.util.function.Supplier;

import org.infinispan.commons.api.BasicCache;

/**
 * @author Paul Ferraro
 */
public abstract class CacheBean implements Cache, Supplier<BasicCache<Key, Value>> {

    @Override
    public String get(String key) {
        Value value = this.get().get(new Key(key));
        return (value != null) ? value.getValue() : null;
    }

    @Override
    public String put(String key, String value) {
        Value old = this.get().put(new Key(key), new Value(value));
        return (old != null) ? old.getValue() : null;
    }

    @Override
    public String remove(String key) {
        Value old = this.get().remove(new Key(key));
        return (old != null) ? old.getValue() : null;
    }
}
