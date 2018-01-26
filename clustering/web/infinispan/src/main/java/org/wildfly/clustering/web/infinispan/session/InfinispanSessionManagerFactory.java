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

import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheProperties;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.infinispan.spi.distribution.SimpleLocality;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueMarshaller;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.infinispan.AffinityIdentifierFactory;
import org.wildfly.clustering.web.infinispan.session.coarse.CoarseSessionAttributesFactory;
import org.wildfly.clustering.web.infinispan.session.fine.FineSessionAttributesFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Factory for creating session managers.
 * @author Paul Ferraro
 */
@Listener
public class InfinispanSessionManagerFactory<C extends Marshallability, L> implements SessionManagerFactory<L, TransactionBatch> {

    private static ThreadFactory createThreadFactory() {
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(new ThreadGroup(InfinispanSessionManager.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
        return WildFlySecurityManager.doUnchecked(action);
    }

    final Batcher<TransactionBatch> batcher;
    final Registrar<SessionExpirationListener> expirationRegistrar;
    final CommandDispatcher<Scheduler> dispatcher;
    final NodeFactory<Address> memberFactory;
    final CacheProperties properties;
    final Cache<Key<String>, ?> cache;
    final Group group;

    private final KeyAffinityServiceFactory affinityFactory;
    private final SessionFactory<InfinispanSessionMetaData<L>, ?, L> factory;
    private final Scheduler scheduler;
    private final SessionCreationMetaDataKeyFilter filter = new SessionCreationMetaDataKeyFilter();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(createThreadFactory());
    private final AtomicReference<Future<?>> rehashFuture = new AtomicReference<>();

    public InfinispanSessionManagerFactory(InfinispanSessionManagerFactoryConfiguration<C, L> config) {
        this.affinityFactory = config.getKeyAffinityServiceFactory();
        this.cache = config.getCache();
        this.memberFactory = config.getMemberFactory();
        this.batcher = new InfinispanBatcher(this.cache);
        this.properties = new InfinispanCacheProperties(this.cache.getCacheConfiguration());
        SessionMetaDataFactory<InfinispanSessionMetaData<L>, L> metaDataFactory = new InfinispanSessionMetaDataFactory<>(config.getCache(), this.properties);
        this.factory = new InfinispanSessionFactory<>(metaDataFactory, this.createSessionAttributesFactory(config), config.getSessionManagerFactoryConfiguration().getLocalContextFactory());
        CommandDispatcherFactory dispatcherFactory = config.getCommandDispatcherFactory();
        ExpiredSessionRemover<?, ?, L> remover = new ExpiredSessionRemover<>(this.factory);
        this.expirationRegistrar = remover;
        this.scheduler = new SessionExpirationScheduler(this.batcher, remover);
        this.dispatcher = dispatcherFactory.createCommandDispatcher(this.cache.getName(), this.scheduler);
        this.group = dispatcherFactory.getGroup();
        this.cache.addListener(this);
        this.schedule(new SimpleLocality(false), new CacheLocality(this.cache));
    }

    @Override
    public SessionManager<L, TransactionBatch> createSessionManager(final SessionManagerConfiguration configuration) {
        IdentifierFactory<String> factory = new AffinityIdentifierFactory<>(configuration.getIdentifierFactory(), this.cache, this.affinityFactory);
        InfinispanSessionManagerConfiguration config = new InfinispanSessionManagerConfiguration() {
            @Override
            public SessionExpirationListener getExpirationListener() {
                return configuration.getExpirationListener();
            }

            @Override
            public ServletContext getServletContext() {
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
            public CommandDispatcher<Scheduler> getCommandDispatcher() {
                return InfinispanSessionManagerFactory.this.dispatcher;
            }

            @Override
            public Registrar<SessionExpirationListener> getExpirationRegistar() {
                return InfinispanSessionManagerFactory.this.expirationRegistrar;
            }

            @Override
            public Group getGroup() {
                return InfinispanSessionManagerFactory.this.group;
            }

            @Override
            public NodeFactory<Address> getMemberFactory() {
                return InfinispanSessionManagerFactory.this.memberFactory;
            }

            @Override
            public Recordable<ImmutableSession> getInactiveSessionRecorder() {
                return configuration.getInactiveSessionRecorder();
            }
        };
        return new InfinispanSessionManager<>(this.factory, config);
    }

    private SessionAttributesFactory<?> createSessionAttributesFactory(InfinispanSessionManagerFactoryConfiguration<C, L> configuration) {
        SessionManagerFactoryConfiguration<C, L> config = configuration.getSessionManagerFactoryConfiguration();
        MarshalledValueFactory<C> factory = config.getMarshalledValueFactory();
        C context = config.getMarshallingContext();

        switch (config.getAttributePersistenceStrategy()) {
            case FINE: {
                return new FineSessionAttributesFactory<>(configuration.getCache(), configuration.getCache(), new MarshalledValueMarshaller<>(factory, context), this.properties);
            }
            case COARSE: {
                return new CoarseSessionAttributesFactory<>(configuration.getCache(), new MarshalledValueMarshaller<>(factory, context), this.properties);
            }
            default: {
                // Impossible
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public void close() {
        this.cache.removeListener(this);
        PrivilegedAction<List<Runnable>> action = () -> this.executor.shutdownNow();
        WildFlySecurityManager.doUnchecked(action);
        try {
            this.executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.dispatcher.close();
        this.scheduler.close();
    }

    @DataRehashed
    public void dataRehashed(DataRehashedEvent<SessionCreationMetaDataKey, ?> event) {
        Cache<SessionCreationMetaDataKey, ?> cache = event.getCache();
        Locality newLocality = new ConsistentHashLocality(cache, event.getConsistentHashAtEnd());
        if (event.isPre()) {
            Future<?> future = this.rehashFuture.getAndSet(null);
            if (future != null) {
                future.cancel(true);
            }
            try {
                this.executor.submit(() -> this.dispatcher.getContext().cancel(newLocality));
            } catch (RejectedExecutionException e) {
                // Executor was shutdown
            }
        } else {
            Locality oldLocality = new ConsistentHashLocality(cache, event.getConsistentHashAtStart());
            try {
                this.rehashFuture.set(this.executor.submit(() -> this.schedule(oldLocality, newLocality)));
            } catch (RejectedExecutionException e) {
                // Executor was shutdown
            }
        }
    }

    private void schedule(Locality oldLocality, Locality newLocality) {
        SessionMetaDataFactory<InfinispanSessionMetaData<L>, L> metaDataFactory = this.factory.getMetaDataFactory();
        // Iterate over sessions in memory
        try (CloseableIterator<Key<String>> keys = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).keySet().iterator()) {
            while (keys.hasNext()) {
                if (Thread.currentThread().isInterrupted()) break;
                Key<String> key = keys.next();
                // If we are the new primary owner of this session then schedule expiration of this session locally
                if (this.filter.test(key) && !oldLocality.isLocal(key) && newLocality.isLocal(key)) {
                    String id = key.getValue();
                    try (Batch batch = this.batcher.createBatch()) {
                        try {
                            // We need to lookup the session to obtain its meta data
                            InfinispanSessionMetaData<L> value = metaDataFactory.tryValue(id);
                            if (value != null) {
                                this.scheduler.schedule(id, metaDataFactory.createImmutableSessionMetaData(id, value));
                            }
                            return;
                        } catch (CacheException e) {
                            batch.discard();
                        }
                    }
                }
            }
        }
    }
}
