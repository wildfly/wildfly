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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.infinispan.Cache;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.ConcurrentManager;
import org.wildfly.clustering.ee.cache.SimpleManager;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.PrimaryOwnerLocator;
import org.wildfly.clustering.ee.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleLocalKeysTask;
import org.wildfly.clustering.ee.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.SchedulerListener;
import org.wildfly.clustering.ee.infinispan.scheduler.SchedulerTopologyChangeListener;
import org.wildfly.clustering.ee.infinispan.tx.InfinispanBatcher;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.infinispan.spi.distribution.SimpleLocality;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.ConcurrentSessionManager;
import org.wildfly.clustering.web.cache.session.MarshalledValueSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.cache.session.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.SessionMetaDataFactory;
import org.wildfly.clustering.web.infinispan.AffinityIdentifierFactory;
import org.wildfly.clustering.web.infinispan.session.coarse.CoarseSessionAttributesFactory;
import org.wildfly.clustering.web.infinispan.session.fine.FineSessionAttributesFactory;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SpecificationProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Factory for creating session managers.
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <MC> the marshalling context type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public class InfinispanSessionManagerFactory<S, SC, AL, MC, LC> implements SessionManagerFactory<SC, LC, TransactionBatch>, Runnable {

    final Batcher<TransactionBatch> batcher;
    final Registrar<SessionExpirationListener> expirationRegistrar;
    final CacheProperties properties;
    final Cache<Key<String>, ?> cache;
    final org.wildfly.clustering.ee.Scheduler<String, ImmutableSessionMetaData> scheduler;
    final SpecificationProvider<S, SC, AL> provider;
    final ExecutorService executor = Executors.newCachedThreadPool(new DefaultThreadFactory(this.getClass()));
    final SessionAttributeActivationNotifierFactory<S, SC, AL, LC, TransactionBatch> notifierFactory;

    private final KeyAffinityServiceFactory affinityFactory;
    private final SessionFactory<SC, CompositeSessionMetaDataEntry<LC>, ?, LC> factory;
    private final BiConsumer<Locality, Locality> scheduleTask;
    private final SchedulerListener listener;

    public InfinispanSessionManagerFactory(InfinispanSessionManagerFactoryConfiguration<S, SC, AL, MC, LC> config) {
        this.affinityFactory = config.getKeyAffinityServiceFactory();
        this.cache = config.getCache();
        this.batcher = new InfinispanBatcher(this.cache);
        this.properties = config.getCacheProperties();
        this.provider = config.getSpecificationProvider();
        this.notifierFactory = new SessionAttributeActivationNotifierFactory<>(this.provider);
        InfinispanSessionMetaDataFactoryConfiguration metaDataFactoryConfig = new InfinispanSessionMetaDataFactoryConfiguration() {
            @Override
            public <K, V> Cache<K, V> getCache() {
                return config.getCache();
            }

            @Override
            public Executor getExecutor() {
                return InfinispanSessionManagerFactory.this.executor;
            }
        };
        SessionMetaDataFactory<CompositeSessionMetaDataEntry<LC>> metaDataFactory = this.properties.isLockOnRead() ? new LockOnReadInfinispanSessionMetaDataFactory<>(metaDataFactoryConfig) : new InfinispanSessionMetaDataFactory<>(metaDataFactoryConfig);
        this.factory = new CompositeSessionFactory<>(metaDataFactory, this.createSessionAttributesFactory(config), config.getLocalContextFactory());
        ExpiredSessionRemover<SC, ?, ?, LC> remover = new ExpiredSessionRemover<>(this.factory);
        this.expirationRegistrar = remover;
        Scheduler<String, ImmutableSessionMetaData> localScheduler = new SessionExpirationScheduler<>(this.batcher, this.factory.getMetaDataFactory(), remover, Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout()));
        CommandDispatcherFactory dispatcherFactory = config.getCommandDispatcherFactory();
        Group group = dispatcherFactory.getGroup();
        this.scheduler = group.isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(dispatcherFactory, this.cache.getName(), localScheduler, new PrimaryOwnerLocator<>(this.cache, config.getMemberFactory()), SessionCreationMetaDataKey::new);

        this.scheduleTask = new ScheduleLocalKeysTask<>(this.cache, SessionCreationMetaDataKeyFilter.INSTANCE, localScheduler);
        this.listener = new SchedulerTopologyChangeListener<>(this.cache, localScheduler, this.scheduleTask);
    }

    @Override
    public void run() {
        this.scheduleTask.accept(new SimpleLocality(false), new CacheLocality(this.cache));
    }

    @Override
    public SessionManager<LC, TransactionBatch> createSessionManager(final SessionManagerConfiguration<SC> configuration) {
        IdentifierFactory<String> factory = new AffinityIdentifierFactory<>(configuration.getIdentifierFactory(), this.cache, this.affinityFactory);
        InfinispanSessionManagerConfiguration<SC, LC> config = new InfinispanSessionManagerConfiguration<SC, LC>() {
            @Override
            public SessionExpirationListener getExpirationListener() {
                return configuration.getExpirationListener();
            }

            @Override
            public SC getServletContext() {
                return configuration.getServletContext();
            }

            @Override
            public Cache<Key<String>, ?> getCache() {
                return InfinispanSessionManagerFactory.this.cache;
            }

            @Override
            public CacheProperties getProperties() {
                return InfinispanSessionManagerFactory.this.properties;
            }

            @Override
            public IdentifierFactory<String> getIdentifierFactory() {
                return factory;
            }

            @Override
            public Batcher<TransactionBatch> getBatcher() {
                return InfinispanSessionManagerFactory.this.batcher;
            }

            @Override
            public Registrar<SessionExpirationListener> getExpirationRegistar() {
                return InfinispanSessionManagerFactory.this.expirationRegistrar;
            }

            @Override
            public Recordable<ImmutableSessionMetaData> getInactiveSessionRecorder() {
                return configuration.getInactiveSessionRecorder();
            }

            @Override
            public org.wildfly.clustering.ee.Scheduler<String, ImmutableSessionMetaData> getExpirationScheduler() {
                return InfinispanSessionManagerFactory.this.scheduler;
            }

            @Override
            public Runnable getStartTask() {
                return InfinispanSessionManagerFactory.this;
            }

            @Override
            public Registrar<Map.Entry<SC, SessionManager<LC, TransactionBatch>>> getContextRegistrar() {
                return InfinispanSessionManagerFactory.this.notifierFactory;
            }
        };
        return new ConcurrentSessionManager<>(new InfinispanSessionManager<>(this.factory, config), this.properties.isTransactional() ? SimpleManager::new : ConcurrentManager::new);
    }

    private SessionAttributesFactory<SC, ?> createSessionAttributesFactory(InfinispanSessionManagerFactoryConfiguration<S, SC, AL, MC, LC> configuration) {
        switch (configuration.getAttributePersistenceStrategy()) {
            case FINE: {
                return new FineSessionAttributesFactory<>(new InfinispanMarshalledValueSessionAttributesFactoryConfiguration<>(configuration, this.notifierFactory, this.executor));
            }
            case COARSE: {
                return new CoarseSessionAttributesFactory<>(new InfinispanMarshalledValueSessionAttributesFactoryConfiguration<>(configuration, this.notifierFactory, this.executor));
            }
            default: {
                // Impossible
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public void close() {
        this.listener.close();
        this.scheduler.close();
        this.factory.close();
        WildFlySecurityManager.doUnchecked(this.executor, DefaultExecutorService.SHUTDOWN_ACTION);
        try {
            this.executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class InfinispanMarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, MC, LC> extends MarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, MC, LC> implements InfinispanSessionAttributesFactoryConfiguration<S, SC, AL, V, MarshalledValue<V, MC>> {
        private final InfinispanSessionManagerFactoryConfiguration<S, SC, AL, MC, LC> configuration;
        private final Function<String, SessionAttributeActivationNotifier> notifierFactory;
        private final Executor executor;

        InfinispanMarshalledValueSessionAttributesFactoryConfiguration(InfinispanSessionManagerFactoryConfiguration<S, SC, AL, MC, LC> configuration, Function<String, SessionAttributeActivationNotifier> notifierFactory, Executor executor) {
            super(configuration);
            this.configuration = configuration;
            this.notifierFactory = notifierFactory;
            this.executor = executor;
        }

        @Override
        public <CK, CV> Cache<CK, CV> getCache() {
            return this.configuration.getCache();
        }

        @Override
        public Executor getExecutor() {
            return this.executor;
        }

        @Override
        public Function<String, SessionAttributeActivationNotifier> getActivationNotifierFactory() {
            return this.notifierFactory;
        }
    }
}
