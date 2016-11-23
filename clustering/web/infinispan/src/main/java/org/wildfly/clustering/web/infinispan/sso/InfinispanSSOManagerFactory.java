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

import java.io.Externalizable;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractService;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleClassTable;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueMarshaller;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.AffinityIdentifierFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSessionsFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactory<A, D> extends AbstractService<SSOManagerFactory<A, D, TransactionBatch>> implements SSOManagerFactory<A, D, TransactionBatch> {

    enum MarshallingVersion implements Function<ModuleLoader, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(ModuleLoader loader) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(loader));
                config.setClassTable(new SimpleClassTable(IndexExternalizer.UNSIGNED_BYTE, Serializable.class, Externalizable.class));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_1;
    }

    private final InfinispanSSOManagerFactoryConfiguration configuration;

    public InfinispanSSOManagerFactory(InfinispanSSOManagerFactoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <L> SSOManager<A, D, L, TransactionBatch> createSSOManager(IdentifierFactory<String> identifierFactory, LocalContextFactory<L> localContextFactory) {
        MarshallingContext marshallingContext = new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, this.configuration.getModuleLoader()), null);
        MarshalledValueFactory<MarshallingContext> marshalledValueFactory = new SimpleMarshalledValueFactory(marshallingContext);
        Marshaller<A, MarshalledValue<A, MarshallingContext>> marshaller = new MarshalledValueMarshaller<>(marshalledValueFactory, marshallingContext);
        Cache<Key<String>, ?> cache = this.configuration.getCache();
        Configuration config = cache.getCacheConfiguration();
        boolean lockOnRead = cache.getCacheConfiguration().transaction().transactionMode().isTransactional() && (config.transaction().lockingMode() == LockingMode.PESSIMISTIC) && config.locking().isolationLevel() == IsolationLevel.REPEATABLE_READ;
        SessionsFactory<Map<D, String>, D> sessionsFactory = new CoarseSessionsFactory<>(this.configuration.getCache());
        SSOFactory<Map.Entry<A, AtomicReference<L>>, Map<D, String>, A, D, L> factory = new InfinispanSSOFactory<>(this.configuration.getCache(), marshaller, localContextFactory, sessionsFactory, lockOnRead);
        IdentifierFactory<String> idFactory = new AffinityIdentifierFactory<>(identifierFactory, cache, this.configuration.getKeyAffinityServiceFactory());
        Batcher<TransactionBatch> batcher = new InfinispanBatcher(cache);
        return new InfinispanSSOManager<>(factory, idFactory, batcher);
    }
}
