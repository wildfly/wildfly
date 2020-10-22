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
package org.wildfly.clustering.web.hotrod.session;

import java.time.Duration;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.ConcurrentManager;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.hotrod.tx.HotRodBatcher;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.ConcurrentSessionManager;
import org.wildfly.clustering.web.cache.session.MarshalledValueSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.SessionMetaDataFactory;
import org.wildfly.clustering.web.hotrod.session.coarse.CoarseSessionAttributesFactory;
import org.wildfly.clustering.web.hotrod.session.fine.FineSessionAttributesFactory;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;

/**
 * Factory for creating session managers.
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <LC> the local context type
 * @param <MC> the marshalling context type
 * @author Paul Ferraro
 */
public class HotRodSessionManagerFactory<S, SC, AL, MC, LC> implements SessionManagerFactory<SC, LC, TransactionBatch> {

    final Registrar<SessionExpirationListener> expirationRegistrar;
    final Scheduler<String, ImmutableSessionMetaData> expirationScheduler;
    final Batcher<TransactionBatch> batcher;
    final Duration transactionTimeout;
    private final SessionFactory<SC, CompositeSessionMetaDataEntry<LC>, ?, LC> sessionFactory;

    public HotRodSessionManagerFactory(HotRodSessionManagerFactoryConfiguration<S, SC, AL, MC, LC> config) {
        SessionMetaDataFactory<CompositeSessionMetaDataEntry<LC>> metaDataFactory = new HotRodSessionMetaDataFactory<>(config);
        this.sessionFactory = new CompositeSessionFactory<>(metaDataFactory, this.createSessionAttributesFactory(config), config.getLocalContextFactory());
        ExpiredSessionRemover<SC, CompositeSessionMetaDataEntry<LC>, ?, LC> remover = new ExpiredSessionRemover<>(this.sessionFactory);
        this.expirationRegistrar = remover;
        this.batcher = new HotRodBatcher(config.getCache());
        this.expirationScheduler = new SessionExpirationScheduler(this.batcher, remover, Duration.ofMillis(config.getCache().getRemoteCacheManager().getConfiguration().transaction().timeout()));
        this.transactionTimeout = Duration.ofMillis(config.getCache().getRemoteCacheManager().getConfiguration().transaction().timeout());
    }

    @Override
    public SessionManager<LC, TransactionBatch> createSessionManager(SessionManagerConfiguration<SC> configuration) {
        HotRodSessionManagerConfiguration<SC> config = new HotRodSessionManagerConfiguration<SC>() {
            @Override
            public SessionExpirationListener getExpirationListener() {
                return configuration.getExpirationListener();
            }

            @Override
            public Registrar<SessionExpirationListener> getExpirationRegistrar() {
                return HotRodSessionManagerFactory.this.expirationRegistrar;
            }

            @Override
            public Scheduler<String, ImmutableSessionMetaData> getExpirationScheduler() {
                return HotRodSessionManagerFactory.this.expirationScheduler;
            }

            @Override
            public IdentifierFactory<String> getIdentifierFactory() {
                return configuration.getIdentifierFactory();
            }

            @Override
            public SC getServletContext() {
                return configuration.getServletContext();
            }

            @Override
            public Batcher<TransactionBatch> getBatcher() {
                return HotRodSessionManagerFactory.this.batcher;
            }

            @Override
            public Duration getStopTimeout() {
                return HotRodSessionManagerFactory.this.transactionTimeout;
            }
        };
        return new ConcurrentSessionManager<>(new HotRodSessionManager<>(this.sessionFactory, config), ConcurrentManager::new);
    }

    @Override
    public void close() {
        this.expirationScheduler.close();
    }

    private SessionAttributesFactory<SC, ?> createSessionAttributesFactory(HotRodSessionManagerFactoryConfiguration<S, SC, AL, MC, LC> configuration) {
        switch (configuration.getAttributePersistenceStrategy()) {
            case FINE: {
                return new FineSessionAttributesFactory<>(new HotRodMarshalledValueSessionAttributesFactoryConfiguration<>(configuration));
            }
            case COARSE: {
                return new CoarseSessionAttributesFactory<>(new HotRodMarshalledValueSessionAttributesFactoryConfiguration<>(configuration));
            }
            default: {
                // Impossible
                throw new IllegalStateException();
            }
        }
    }

    private static class HotRodMarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, MC, LC> extends MarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, MC, LC> implements HotRodSessionAttributesFactoryConfiguration<S, SC, AL, V, MarshalledValue<V, MC>> {
        private final HotRodSessionManagerFactoryConfiguration<S, SC, AL, MC, LC> configuration;

        HotRodMarshalledValueSessionAttributesFactoryConfiguration(HotRodSessionManagerFactoryConfiguration<S, SC, AL, MC, LC> configuration) {
            super(configuration);
            this.configuration = configuration;
        }

        @Override
        public <CK, CV> RemoteCache<CK, CV> getCache() {
            return this.configuration.getCache();
        }
    }
}
