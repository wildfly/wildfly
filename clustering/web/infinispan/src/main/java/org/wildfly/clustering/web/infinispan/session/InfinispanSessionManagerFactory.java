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
package org.wildfly.clustering.web.infinispan.session;

import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.infinispan.Cache;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.ConcurrentManager;
import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.cache.SimpleManager;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ee.infinispan.PrimaryOwnerLocator;
import org.wildfly.clustering.ee.infinispan.affinity.AffinityIdentifierFactory;
import org.wildfly.clustering.ee.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleLocalKeysTask;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleWithMetaDataCommand;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleWithTransientMetaDataCommand;
import org.wildfly.clustering.ee.infinispan.scheduler.CacheEntryScheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.SchedulerTopologyChangeListener;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.distribution.Locality;
import org.wildfly.clustering.infinispan.distribution.SimpleLocality;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.ConcurrentSessionManager;
import org.wildfly.clustering.web.cache.session.MarshalledValueSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.cache.session.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.SessionMetaDataFactory;
import org.wildfly.clustering.web.infinispan.session.coarse.CoarseSessionAttributesFactory;
import org.wildfly.clustering.web.infinispan.session.fine.FineSessionAttributesFactory;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionExpirationMetaData;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SpecificationProvider;

/**
 * Factory for creating session managers.
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public class InfinispanSessionManagerFactory<S, SC, AL, LC> implements SessionManagerFactory<SC, LC, TransactionBatch>, Runnable {

    private final org.wildfly.clustering.ee.Scheduler<String, SessionExpirationMetaData> scheduler;
    private final SpecificationProvider<S, SC, AL> provider;
    private final KeyAffinityServiceFactory affinityFactory;
    private final SessionFactory<SC, CompositeSessionMetaDataEntry<LC>, ?, LC> factory;
    private final BiConsumer<Locality, Locality> scheduleTask;
    private final ListenerRegistration schedulerListenerRegistration;
    private final InfinispanConfiguration configuration;
    private final ExpiredSessionRemover<SC, ?, ?, LC> remover;
    private final SessionAttributeActivationNotifierFactory<S, SC, AL, LC, TransactionBatch> notifierFactory;

    public InfinispanSessionManagerFactory(InfinispanSessionManagerFactoryConfiguration<S, SC, AL, LC> config) {
        this.configuration = config;
        this.affinityFactory = config.getKeyAffinityServiceFactory();
        this.provider = config.getSpecificationProvider();
        this.notifierFactory = new SessionAttributeActivationNotifierFactory<>(this.provider);
        CacheProperties properties = config.getCacheProperties();
        SessionMetaDataFactory<CompositeSessionMetaDataEntry<LC>> metaDataFactory = properties.isLockOnRead() ? new LockOnReadInfinispanSessionMetaDataFactory<>(config) : new BulkReadInfinispanSessionMetaDataFactory<>(config);
        this.factory = new CompositeSessionFactory<>(metaDataFactory, this.createSessionAttributesFactory(config), config.getLocalContextFactory());
        this.remover = new ExpiredSessionRemover<>(this.factory);
        Cache<Key<String>, ?> cache = config.getCache();
        CacheEntryScheduler<String, SessionExpirationMetaData> localScheduler = new SessionExpirationScheduler<>(config.getBatcher(), this.factory.getMetaDataFactory(), this.remover, Duration.ofMillis(cache.getCacheConfiguration().transaction().cacheStopTimeout()));
        CommandDispatcherFactory dispatcherFactory = config.getCommandDispatcherFactory();
        Group group = dispatcherFactory.getGroup();
        this.scheduler = group.isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(dispatcherFactory, cache.getName(), localScheduler, new PrimaryOwnerLocator<>(cache, config.getMemberFactory()), SessionCreationMetaDataKey::new, properties.isTransactional() ? ScheduleWithMetaDataCommand::new : ScheduleWithTransientMetaDataCommand::new);

        this.scheduleTask = new ScheduleLocalKeysTask<>(cache, SessionCreationMetaDataKeyFilter.INSTANCE, localScheduler);
        this.schedulerListenerRegistration = new SchedulerTopologyChangeListener<>(cache, localScheduler, this.scheduleTask).register();
    }

    @Override
    public void run() {
        this.scheduleTask.accept(new SimpleLocality(false), new CacheLocality(this.configuration.getCache()));
    }

    @Override
    public SessionManager<LC, TransactionBatch> createSessionManager(final SessionManagerConfiguration<SC> configuration) {
        IdentifierFactory<String> factory = new AffinityIdentifierFactory<>(configuration.getIdentifierFactory(), this.configuration.getCache(), this.affinityFactory);
        Registrar<SessionManager<LC, TransactionBatch>> registrar = manager -> {
            Registration contextRegistration = this.notifierFactory.register(Map.entry(configuration.getServletContext(), manager));
            Registration expirationRegistration = this.remover.register(configuration.getExpirationListener());
            return () -> {
                expirationRegistration.close();
                contextRegistration.close();
            };
        };
        org.wildfly.clustering.ee.Scheduler<String, SessionExpirationMetaData> scheduler = this.scheduler;
        InfinispanSessionManagerConfiguration<SC, LC> config = new AbstractInfinispanSessionManagerConfiguration<>(this.configuration) {
            @Override
            public SessionExpirationListener getExpirationListener() {
                return configuration.getExpirationListener();
            }

            @Override
            public SC getServletContext() {
                return configuration.getServletContext();
            }

            @Override
            public IdentifierFactory<String> getIdentifierFactory() {
                return factory;
            }

            @Override
            public org.wildfly.clustering.ee.Scheduler<String, SessionExpirationMetaData> getExpirationScheduler() {
                return scheduler;
            }

            @Override
            public Runnable getStartTask() {
                return InfinispanSessionManagerFactory.this;
            }

            @Override
            public Registrar<SessionManager<LC, TransactionBatch>> getRegistrar() {
                return registrar;
            }
        };
        return new ConcurrentSessionManager<>(new InfinispanSessionManager<>(this.factory, config), this.configuration.getCacheProperties().isTransactional() ? SimpleManager::new : ConcurrentManager::new);
    }

    private SessionAttributesFactory<SC, ?> createSessionAttributesFactory(InfinispanSessionManagerFactoryConfiguration<S, SC, AL, LC> configuration) {
        switch (configuration.getAttributePersistenceStrategy()) {
            case FINE: {
                return new FineSessionAttributesFactory<>(new InfinispanMarshalledValueSessionAttributesFactoryConfiguration<>(configuration, this.notifierFactory));
            }
            case COARSE: {
                return new CoarseSessionAttributesFactory<>(new InfinispanMarshalledValueSessionAttributesFactoryConfiguration<>(configuration, this.notifierFactory));
            }
            default: {
                // Impossible
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public void close() {
        this.schedulerListenerRegistration.close();
        this.scheduler.close();
        this.factory.close();
    }

    private abstract static class AbstractInfinispanSessionManagerConfiguration<SC, LC> implements InfinispanSessionManagerConfiguration<SC, LC> {
        private final InfinispanConfiguration configuration;

        AbstractInfinispanSessionManagerConfiguration(InfinispanConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public <K, V> Cache<K, V> getCache() {
            return this.configuration.getCache();
        }
    }

    private static class InfinispanMarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, LC> extends MarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, LC> implements InfinispanSessionAttributesFactoryConfiguration<S, SC, AL, V, MarshalledValue<V, ByteBufferMarshaller>> {
        private final InfinispanSessionManagerFactoryConfiguration<S, SC, AL, LC> configuration;
        private final Function<String, SessionAttributeActivationNotifier> notifierFactory;

        InfinispanMarshalledValueSessionAttributesFactoryConfiguration(InfinispanSessionManagerFactoryConfiguration<S, SC, AL, LC> configuration, Function<String, SessionAttributeActivationNotifier> notifierFactory) {
            super(configuration);
            this.configuration = configuration;
            this.notifierFactory = notifierFactory;
        }

        @Override
        public <CK, CV> Cache<CK, CV> getCache() {
            return this.configuration.getCache();
        }

        @Override
        public Function<String, SessionAttributeActivationNotifier> getActivationNotifierFactory() {
            return this.notifierFactory;
        }
    }
}
