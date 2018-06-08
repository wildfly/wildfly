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
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheProperties;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.marshalling.spi.MarshalledValueMarshaller;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.infinispan.AffinityIdentifierFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSessionsFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerConfiguration;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactory<A, D, S> implements SSOManagerFactory<A, D, S, TransactionBatch> {

    private final InfinispanSSOManagerFactoryConfiguration configuration;

    public InfinispanSSOManagerFactory(InfinispanSSOManagerFactoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <L, C extends Marshallability> SSOManager<A, D, S, L, TransactionBatch> createSSOManager(SSOManagerConfiguration<L, C> configuration) {
        Cache<Key<String>, ?> cache = this.configuration.getCache();
        CacheProperties properties = new InfinispanCacheProperties(cache.getCacheConfiguration());
        SessionsFactory<Map<D, S>, D, S> sessionsFactory = new CoarseSessionsFactory<>(this.configuration.getCache(), properties);
        SSOFactory<Map.Entry<A, AtomicReference<L>>, Map<D, S>, A, D, S, L> factory = new InfinispanSSOFactory<>(this.configuration.getCache(), properties, new MarshalledValueMarshaller<>(configuration.getMarshalledValueFactory(), configuration.getMarshallingContext()), configuration.getLocalContextFactory(), sessionsFactory);
        IdentifierFactory<String> idFactory = new AffinityIdentifierFactory<>(configuration.getIdentifierFactory(), cache, this.configuration.getKeyAffinityServiceFactory());
        Batcher<TransactionBatch> batcher = new InfinispanBatcher(cache);
        return new InfinispanSSOManager<>(factory, idFactory, batcher);
    }
}
