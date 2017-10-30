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

import javax.servlet.ServletContext;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheProperties;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueMarshaller;
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

/**
 * Factory for creating session managers.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagerFactory<C extends Marshallability, L> implements SessionManagerFactory<L, TransactionBatch>, AutoCloseable {
    final Batcher<TransactionBatch> batcher;
    final Registrar<SessionExpirationListener> sessionExpirationListenerRegistrar;
    final CommandDispatcher<Scheduler> dispatcher;
    final NodeFactory<Address> nodeFactory;
    final CacheProperties properties;
    final Cache<Key<String>, ?> cache;
    final Group group;
    private final KeyAffinityServiceFactory affinityFactory;
    private final SessionFactory<?, ?, L> factory;
    private final Scheduler scheduler;

    public InfinispanSessionManagerFactory(InfinispanSessionManagerFactoryConfiguration<C, L> config) {
        this.affinityFactory = config.getKeyAffinityServiceFactory();
        this.cache = config.getCache();
        this.nodeFactory = config.getNodeFactory();
        this.batcher = new InfinispanBatcher(this.cache);
        this.properties = new InfinispanCacheProperties(this.cache.getCacheConfiguration());
        SessionMetaDataFactory<InfinispanSessionMetaData<L>, L> metaDataFactory = new InfinispanSessionMetaDataFactory<>(config.getCache(), this.properties);
        this.factory = new InfinispanSessionFactory<>(metaDataFactory, this.createSessionAttributesFactory(config), config.getSessionManagerFactoryConfiguration().getLocalContextFactory());
        ExpiredSessionRemover<?, ?, L> remover = new ExpiredSessionRemover<>(this.factory);
        this.sessionExpirationListenerRegistrar = remover;
        this.scheduler = new SessionExpirationScheduler(this.batcher, remover);
        CommandDispatcherFactory dispatcherFactory = config.getCommandDispatcherFactory();
        this.dispatcher = dispatcherFactory.createCommandDispatcher(this.cache.getName(), this.scheduler);
        this.group = dispatcherFactory.getGroup();
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
            public NodeFactory<Address> getNodeFactory() {
                return InfinispanSessionManagerFactory.this.nodeFactory;
            }

            @Override
            public Recordable<ImmutableSession> getInactiveSessionRecorder() {
                return configuration.getInactiveSessionRecorder();
            }

            @Override
            public Registrar<SessionExpirationListener> getSessionExpirationListenerRegistrar() {
                return InfinispanSessionManagerFactory.this.sessionExpirationListenerRegistrar;
            }

            @Override
            public Group getGroup() {
                return InfinispanSessionManagerFactory.this.group;
            }
        };
        return new InfinispanSessionManager<>(this.factory, config);
    }

    private SessionAttributesFactory<?> createSessionAttributesFactory(InfinispanSessionManagerFactoryConfiguration<C, L> configuration) {
        SessionManagerFactoryConfiguration<C, L> config = configuration.getSessionManagerFactoryConfiguration();
        MarshalledValueFactory<C> factory = config.getMarshalledValueFactory();
        C context = config.getMarshallingContext();

        switch (configuration.getSessionManagerFactoryConfiguration().getAttributePersistenceStrategy()) {
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
        this.dispatcher.close();
        this.scheduler.close();
    }
}
