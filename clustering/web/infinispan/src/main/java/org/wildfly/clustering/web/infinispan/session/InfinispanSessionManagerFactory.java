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

import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.marshalling.MarshalledValue;
import org.jboss.as.clustering.marshalling.MarshalledValueFactory;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.as.clustering.marshalling.SimpleMarshalledValueFactory;
import org.jboss.as.clustering.marshalling.SimpleMarshallingContextFactory;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.spi.CacheServiceNames;
import org.wildfly.clustering.spi.ChannelServiceNames;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.InfinispanBatcher;
import org.wildfly.clustering.web.infinispan.AffinityIdentifierFactory;
import org.wildfly.clustering.web.infinispan.session.coarse.CoarseSessionCacheEntry;
import org.wildfly.clustering.web.infinispan.session.coarse.CoarseSessionFactory;
import org.wildfly.clustering.web.infinispan.session.coarse.SessionAttributesCacheKey;
import org.wildfly.clustering.web.infinispan.session.fine.FineSessionCacheEntry;
import org.wildfly.clustering.web.infinispan.session.fine.FineSessionFactory;
import org.wildfly.clustering.web.infinispan.session.fine.SessionAttributeCacheKey;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;

/**
 * Factory for creating session managers.
 * @author Paul Ferraro
 */
@SuppressWarnings("rawtypes")
public class InfinispanSessionManagerFactory extends AbstractService<SessionManagerFactory> implements SessionManagerFactory {

    public static ServiceBuilder<SessionManagerFactory> build(ServiceTarget target, ServiceName name, String containerName, String cacheName, SessionManagerConfiguration config) {
        InfinispanSessionManagerFactory factory = new InfinispanSessionManagerFactory(config);
        return target.addService(name, factory)
                .addDependency(CacheService.getServiceName(containerName, cacheName), Cache.class, factory.cache)
                .addDependency(KeyAffinityServiceFactoryService.getServiceName(containerName), KeyAffinityServiceFactory.class, factory.affinityFactory)
                .addDependency(ChannelServiceNames.COMMAND_DISPATCHER.getServiceName(containerName), CommandDispatcherFactory.class, factory.dispatcherFactory)
                .addDependency(CacheServiceNames.NODE_FACTORY.getServiceName(containerName), NodeFactory.class, factory.nodeFactory)
        ;
    }

    private final SessionManagerConfiguration config;
    private final CacheInvoker invoker = new RetryingCacheInvoker(10, 100);
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<KeyAffinityServiceFactory> affinityFactory = new InjectedValue<>();
    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
    private final InjectedValue<NodeFactory> nodeFactory = new InjectedValue<>();

    private InfinispanSessionManagerFactory(SessionManagerConfiguration config) {
        this.config = config;
    }

    @Override
    public SessionManagerFactory getValue() {
        return this;
    }

    @Override
    public <L> SessionManager<L> createSessionManager(final SessionContext context, IdentifierFactory<String> identifierFactory, LocalContextFactory<L> localContextFactory) {
        final Batcher batcher = new InfinispanBatcher(this.cache.getValue());
        final IdentifierFactory<String> factory = new AffinityIdentifierFactory<>(identifierFactory, this.cache.getValue(), this.affinityFactory.getValue());
        final Cache<String, ?> cache = this.cache.getValue();
        final CommandDispatcherFactory dispatcherFactory = this.dispatcherFactory.getValue();
        final NodeFactory<Address> nodeFactory = this.nodeFactory.getValue();
        final int maxActiveSessions = this.config.getMaxActiveSessions();
        InfinispanSessionManagerConfiguration config = new InfinispanSessionManagerConfiguration() {
            @Override
            public SessionContext getSessionContext() {
                return context;
            }

            @Override
            public Cache<String, ?> getCache() {
                return cache;
            }

            @Override
            public IdentifierFactory<String> getIdentifierFactory() {
                return factory;
            }

            @Override
            public Batcher getBatcher() {
                return batcher;
            }

            @Override
            public CommandDispatcherFactory getCommandDispatcherFactory() {
                return dispatcherFactory;
            }

            @Override
            public NodeFactory<Address> getNodeFactory() {
                return nodeFactory;
            }

            @Override
            public int getMaxActiveSessions() {
                return maxActiveSessions;
            }
        };
        return new InfinispanSessionManager<>(this.getSessionFactory(context, localContextFactory), config);
    }

    private <L> SessionFactory<?, L> getSessionFactory(SessionContext context, LocalContextFactory<L> localContextFactory) {
        Module module = this.config.getModule();
        MarshallingContext marshallingContext = new SimpleMarshallingContextFactory().createMarshallingContext(new SessionAttributeMarshallingContext(module), module.getClassLoader());
        MarshalledValueFactory<MarshallingContext> factory = new SimpleMarshalledValueFactory(marshallingContext);

        switch (this.config.getAttributePersistenceStrategy()) {
            case FINE: {
                Cache<String, FineSessionCacheEntry<L>> sessionCache = this.cache.getValue();
                Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> attributeCache = this.cache.getValue();
                SessionAttributeMarshaller<Object, MarshalledValue<Object, MarshallingContext>> marshaller = new MarshalledValueSessionAttributeMarshaller<>(factory, marshallingContext);
                return new FineSessionFactory<>(sessionCache, attributeCache, this.invoker, context, marshaller, localContextFactory);
            }
            case COARSE: {
                Cache<String, CoarseSessionCacheEntry<L>> sessionCache = this.cache.getValue();
                Cache<SessionAttributesCacheKey, MarshalledValue<Map<String, Object>, MarshallingContext>> attributesCache = this.cache.getValue();
                SessionAttributeMarshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> marshaller = new MarshalledValueSessionAttributeMarshaller<>(factory, marshallingContext);
                return new CoarseSessionFactory<>(sessionCache, attributesCache, this.invoker, context, marshaller, localContextFactory);
            }
            default: {
                // Impossible
                throw new IllegalStateException();
            }
        }
    }
}
