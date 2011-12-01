/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan;

import org.infinispan.AbstractDelegatingAdvancedCache;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

/**
 * Workaround for ISPN-1583.
 * @author Paul Ferraro
 * @param <K> Cache key type
 * @param <V> Cache value type
 */
public abstract class AbstractAdvancedCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {

    protected AbstractAdvancedCache(AdvancedCache<K, V> cache) {
        super(cache);
    }

    protected abstract AdvancedCache<K, V> wrap(AdvancedCache<K, V> cache);

    @Override
    public AdvancedCache<K, V> withFlags(Flag... flags) {
        return this.wrap(this.cache.withFlags(flags));
    }

    @Override
    public AdvancedCache<K, V> with(ClassLoader classLoader) {
        return this.wrap(this.cache.with(classLoader));
    }
}
