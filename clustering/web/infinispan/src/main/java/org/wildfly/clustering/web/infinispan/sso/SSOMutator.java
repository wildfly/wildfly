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
package org.wildfly.clustering.web.infinispan.sso;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.wildfly.clustering.web.infinispan.Mutator;

/**
 * Mutates an SSO in the distributed cache
 * @author Paul Ferraro
 */
public class SSOMutator<V> implements Mutator {

    private final Cache<String, V> cache;
    private final CacheInvoker invoker;
    private final String id;
    final V value;

    public SSOMutator(Cache<String, V> cache, CacheInvoker invoker, String id, V value) {
        this.cache = cache;
        this.invoker = invoker;
        this.id = id;
        this.value = value;
    }

    @Override
    public void mutate() {
        this.invoker.invoke(this.cache, new MutateOperation<String, V>(this.id, this.value), Flag.IGNORE_RETURN_VALUES);
    }
}
