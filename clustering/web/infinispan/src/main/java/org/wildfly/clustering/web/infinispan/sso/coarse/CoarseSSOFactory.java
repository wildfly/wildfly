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
package org.wildfly.clustering.web.infinispan.sso.coarse;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.sso.InfinispanSSO;
import org.wildfly.clustering.web.infinispan.sso.SSOFactory;
import org.wildfly.clustering.web.infinispan.sso.SSOMutator;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

public class CoarseSSOFactory<L> implements SSOFactory<CoarseSSOCacheEntry<L>, L> {

    private final Cache<String, CoarseSSOCacheEntry<L>> cache;
    private final CacheInvoker invoker;
    private final LocalContextFactory<L> localContextFactory;

    public CoarseSSOFactory(Cache<String, CoarseSSOCacheEntry<L>> cache, CacheInvoker invoker, LocalContextFactory<L> localContextFactory) {
        this.cache = cache;
        this.invoker = invoker;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public SSO<L> createSSO(String id, CoarseSSOCacheEntry<L> value) {
        Mutator mutator = new SSOMutator<>(this.cache, this.invoker, id, value);
        Sessions sessions = new CoarseSessions(value.getSessions(), mutator);
        return new InfinispanSSO<>(id, value, sessions, value.getLocalContext(), this.localContextFactory, this);
    }

    @Override
    public CoarseSSOCacheEntry<L> createValue(String id) {
        CoarseSSOCacheEntry<L> entry = new CoarseSSOCacheEntry<>();
        CoarseSSOCacheEntry<L> existing = this.invoker.invoke(this.cache, new CreateOperation<>(id, entry));
        return (existing != null) ? existing : entry;
    }

    @Override
    public CoarseSSOCacheEntry<L> findValue(String id) {
        return this.invoker.invoke(this.cache, new FindOperation<String, CoarseSSOCacheEntry<L>>(id));
    }

    @Override
    public void remove(String id) {
        this.invoker.invoke(this.cache, new RemoveOperation<String, CoarseSSOCacheEntry<L>>(id), Flag.IGNORE_RETURN_VALUES);
    }
}
