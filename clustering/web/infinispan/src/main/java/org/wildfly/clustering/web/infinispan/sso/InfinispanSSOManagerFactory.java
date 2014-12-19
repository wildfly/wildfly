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

import java.util.Map;

import org.infinispan.Cache;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.AffinityIdentifierFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseAuthenticationEntry;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOEntry;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSessionsKey;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactory<A, D> extends AbstractService<SSOManagerFactory<A, D, TransactionBatch>> implements SSOManagerFactory<A, D, TransactionBatch> {

    public static <A, D> ServiceBuilder<SSOManagerFactory<A, D, TransactionBatch>> build(ServiceTarget target, ServiceName name, String containerName, String cacheName) {
        InfinispanSSOManagerFactory<A, D> service = new InfinispanSSOManagerFactory<>();
        return target.addService(name, service)
                .addDependency(CacheServiceName.CACHE.getServiceName(containerName, cacheName), Cache.class, service.cache)
                .addDependency(CacheContainerServiceName.AFFINITY.getServiceName(containerName), KeyAffinityServiceFactory.class, service.affinityFactory)
        ;
    }

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<KeyAffinityServiceFactory> affinityFactory = new InjectedValue<>();

    private InfinispanSSOManagerFactory() {
        // Hide
    }

    @Override
    public SSOManagerFactory<A, D, TransactionBatch> getValue() {
        return this;
    }

    @Override
    public <L> SSOManager<A, D, L, TransactionBatch> createSSOManager(IdentifierFactory<String> identifierFactory, LocalContextFactory<L> localContextFactory) {
        Cache<String, CoarseAuthenticationEntry<A, D, L>> authenticationCache = this.cache.getValue();
        Cache<CoarseSessionsKey, Map<D, String>> sessionsCache = this.cache.getValue();
        SSOFactory<CoarseSSOEntry<A, D, L>, A, D, L> factory = new CoarseSSOFactory<>(authenticationCache, sessionsCache, localContextFactory);
        IdentifierFactory<String> idFactory = new AffinityIdentifierFactory<>(identifierFactory, authenticationCache, this.affinityFactory.getValue());
        Batcher<TransactionBatch> batcher = new InfinispanBatcher(authenticationCache);
        return new InfinispanSSOManager<>(factory, idFactory, batcher);
    }
}
