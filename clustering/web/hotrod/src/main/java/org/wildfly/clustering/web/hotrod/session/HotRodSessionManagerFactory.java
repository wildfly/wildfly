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
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.cache.ConcurrentManager;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.ConcurrentSessionManager;
import org.wildfly.clustering.web.cache.session.InactiveSessionMetaDataRecorder;
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
 * @author Paul Ferraro
 */
public class HotRodSessionManagerFactory<S, SC, AL, LC> implements SessionManagerFactory<SC, LC, TransactionBatch> {

    private final HotRodConfiguration configuration;
    private final Registrar<SessionExpirationListener> expirationRegistrar;
    private final SessionFactory<SC, CompositeSessionMetaDataEntry<LC>, ?, LC> factory;
    private final InactiveSessionMetaDataRecorder recorder;

    public HotRodSessionManagerFactory(HotRodSessionManagerFactoryConfiguration<S, SC, AL, LC> configuration) {
        this.configuration = configuration;
        this.recorder = new InactiveSessionMetaDataRecorder();
        SessionMetaDataFactory<CompositeSessionMetaDataEntry<LC>> metaDataFactory = new HotRodSessionMetaDataFactory<>(configuration);
        HotRodSessionFactory<SC, ?, LC> sessionFactory = new HotRodSessionFactory<>(configuration, metaDataFactory, this.createSessionAttributesFactory(configuration), configuration.getLocalContextFactory(), this.recorder);
        this.factory = sessionFactory;
        this.expirationRegistrar = sessionFactory;
    }

    @Override
    public SessionManager<LC, TransactionBatch> createSessionManager(SessionManagerConfiguration<SC> configuration) {
        Duration transactionTimeout = Duration.ofMillis(this.configuration.getCache().getRemoteCacheContainer().getConfiguration().transactionTimeout());
        Registrar<SessionManager<LC, TransactionBatch>> registrar = manager -> {
            Registration expirationRegistration = this.expirationRegistrar.register(configuration.getExpirationListener());
            Recordable<ImmutableSessionMetaData> recorder = configuration.getInactiveSessionRecorder();
            if (recorder != null) {
                recorder.reset();
            }
            Registration recorderRegistration = (recorder != null) ? this.recorder.register(recorder) : null;
            return () -> {
                if (recorderRegistration != null) {
                    recorderRegistration.close();
                }
                expirationRegistration.close();
            };
        };
        HotRodSessionManagerConfiguration<SC, LC> config = new AbstractHotRodSessionManagerConfiguration<>(this.configuration) {
            @Override
            public SessionExpirationListener getExpirationListener() {
                return configuration.getExpirationListener();
            }

            @Override
            public Registrar<SessionManager<LC, TransactionBatch>> getRegistrar() {
                return registrar;
            }

            @Override
            public Supplier<String> getIdentifierFactory() {
                return configuration.getIdentifierFactory();
            }

            @Override
            public SC getServletContext() {
                return configuration.getServletContext();
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

    private abstract static class AbstractHotRodSessionManagerConfiguration<SC, LC> implements HotRodSessionManagerConfiguration<SC, LC> {
        private final HotRodConfiguration configuration;

        AbstractHotRodSessionManagerConfiguration(HotRodConfiguration configuration) {
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
