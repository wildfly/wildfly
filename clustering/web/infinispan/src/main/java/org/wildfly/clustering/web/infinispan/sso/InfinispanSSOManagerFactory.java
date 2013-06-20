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
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOCacheEntry;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

@SuppressWarnings("rawtypes")
public class InfinispanSSOManagerFactory extends AbstractService<SSOManagerFactory> implements SSOManagerFactory {

    private final Value<Cache> cache;
    private volatile CacheInvoker invoker = new RetryingCacheInvoker(10, 100);

    public InfinispanSSOManagerFactory(Value<Cache> cache) {
        this.cache = cache;
    }

    @Override
    public SSOManagerFactory getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public <L> SSOManager<L> createSSOManager(LocalContextFactory<L> localContextFactory) {
        Cache<String, CoarseSSOCacheEntry<L>> cache = (Cache<String, CoarseSSOCacheEntry<L>>) this.cache.getValue();
        SSOFactory<CoarseSSOCacheEntry<L>, L> factory = new CoarseSSOFactory<>(cache, this.invoker, localContextFactory);
        return new InfinispanSSOManager<>(factory, cache);
    }
}
