/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.hotrod.session;

import java.time.Duration;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.ee.cache.ConcurrentManager;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.web.cache.session.ConcurrentSessionManager;
import org.wildfly.clustering.web.cache.session.DelegatingSessionManagerConfiguration;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.attributes.MarshalledValueSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.metadata.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionMetaDataEntry;
import org.wildfly.clustering.web.hotrod.session.attributes.CoarseSessionAttributesFactory;
import org.wildfly.clustering.web.hotrod.session.attributes.FineSessionAttributesFactory;
import org.wildfly.clustering.web.hotrod.session.metadata.HotRodSessionMetaDataFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;

/**
 * Factory for creating session managers.
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public class HotRodSessionManagerFactory<S, SC, AL, LC> implements SessionManagerFactory<SC, LC, TransactionBatch> {

    private final HotRodConfiguration configuration;
    private final Registrar<Consumer<ImmutableSession>> expirationListenerRegistrar;
    private final SessionFactory<SC, SessionMetaDataEntry<LC>, ?, LC> factory;

    public HotRodSessionManagerFactory(HotRodSessionManagerFactoryConfiguration<S, SC, AL, LC> configuration) {
        this.configuration = configuration;
        SessionMetaDataFactory<SessionMetaDataEntry<LC>> metaDataFactory = new HotRodSessionMetaDataFactory<>(configuration);
        HotRodSessionFactory<SC, ?, LC> sessionFactory = new HotRodSessionFactory<>(configuration, metaDataFactory, this.createSessionAttributesFactory(configuration), configuration.getLocalContextFactory());
        this.factory = sessionFactory;
        this.expirationListenerRegistrar = sessionFactory;
    }

    @Override
    public SessionManager<LC, TransactionBatch> createSessionManager(SessionManagerConfiguration<SC> configuration) {
        Duration transactionTimeout = Duration.ofMillis(this.configuration.getCache().getRemoteCacheContainer().getConfiguration().transactionTimeout());
        Registrar<Consumer<ImmutableSession>> expirationListenerRegistrar = this.expirationListenerRegistrar;
        HotRodSessionManagerConfiguration<SC> config = new AbstractHotRodSessionManagerConfiguration<>(configuration, this.configuration) {
            @Override
            public Registrar<Consumer<ImmutableSession>> getExpirationListenerRegistrar() {
                return expirationListenerRegistrar;
            }

            @Override
            public Duration getStopTimeout() {
                return transactionTimeout;
            }
        };
        return new ConcurrentSessionManager<>(new HotRodSessionManager<>(this.factory, config), ConcurrentManager::new);
    }

    @Override
    public void close() {
        this.factory.close();
    }

    private SessionAttributesFactory<SC, ?> createSessionAttributesFactory(HotRodSessionManagerFactoryConfiguration<S, SC, AL, LC> configuration) {
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

    private abstract static class AbstractHotRodSessionManagerConfiguration<SC> extends DelegatingSessionManagerConfiguration<SC> implements HotRodSessionManagerConfiguration<SC> {
        private final HotRodConfiguration configuration;

        AbstractHotRodSessionManagerConfiguration(SessionManagerConfiguration<SC> managerConfiguration, HotRodConfiguration configuration) {
            super(managerConfiguration);
            this.configuration = configuration;
        }

        @Override
        public <CK, CV> RemoteCache<CK, CV> getCache() {
            return this.configuration.getCache();
        }
    }

    private static class HotRodMarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, LC> extends MarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, LC> implements HotRodSessionAttributesFactoryConfiguration<S, SC, AL, V, MarshalledValue<V, ByteBufferMarshaller>> {
        private final HotRodSessionManagerFactoryConfiguration<S, SC, AL, LC> configuration;

        HotRodMarshalledValueSessionAttributesFactoryConfiguration(HotRodSessionManagerFactoryConfiguration<S, SC, AL, LC> configuration) {
            super(configuration);
            this.configuration = configuration;
        }

        @Override
        public <CK, CV> RemoteCache<CK, CV> getCache() {
            return this.configuration.getCache();
        }
    }
}
