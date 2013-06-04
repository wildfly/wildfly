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
import org.jboss.as.clustering.MarshalledValue;
import org.jboss.as.clustering.MarshalledValueFactory;
import org.jboss.as.clustering.MarshallingContext;
import org.jboss.as.clustering.SimpleMarshalledValueFactory;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.InfinispanWebMessages;
import org.wildfly.clustering.web.infinispan.session.coarse.CoarseSessionCacheEntry;
import org.wildfly.clustering.web.infinispan.session.coarse.CoarseSessionFactory;
import org.wildfly.clustering.web.infinispan.session.coarse.SessionAttributesCacheKey;
import org.wildfly.clustering.web.infinispan.session.fine.FineSessionCacheEntry;
import org.wildfly.clustering.web.infinispan.session.fine.FineSessionFactory;
import org.wildfly.clustering.web.infinispan.session.fine.SessionAttributeCacheKey;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionIdentifierFactory;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerFactory;

/**
 * Factory for creating session managers.
 * @author Paul Ferraro
 */
@SuppressWarnings("rawtypes")
public class InfinispanSessionManagerFactory extends AbstractService<SessionManagerFactory> implements SessionManagerFactory {
    private final SessionAttributeMarshallingContext context;
    private final JBossWebMetaData metaData;
    private final CacheInvoker invoker = new RetryingCacheInvoker(10, 100);
    private final Value<Cache> cache;
    private final Value<KeyAffinityServiceFactory> affinityFactory;
    private final Value<Registry> registry;

    public InfinispanSessionManagerFactory(Module module, JBossWebMetaData metaData, Value<Cache> cache, Value<KeyAffinityServiceFactory> affinityFactory, Value<Registry> registry) {
        this.context = new SessionAttributeMarshallingContext(module);
        this.cache = cache;
        this.affinityFactory = affinityFactory;
        this.registry = registry;
        this.metaData = metaData;
    }

    @Override
    public SessionManagerFactory getValue() {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <L> SessionManager<L> createSessionManager(SessionContext context, SessionIdentifierFactory identifierFactory, LocalContextFactory<L> localContextFactory) {
        Cache cache = this.cache.getValue();
        return new InfinispanSessionManager<>(context, identifierFactory, cache, this.<L>getSessionFactory(context, localContextFactory, cache), this.affinityFactory.getValue(), this.registry.getValue(), this.metaData);
    }

    private <L> SessionFactory<?, L> getSessionFactory(SessionContext context, LocalContextFactory<L> localContextFactory, Cache<?, ?> cache) {
        MarshallingContext marshallingContext = new MarshallingContext(this.context);
        MarshalledValueFactory<MarshallingContext> factory = new SimpleMarshalledValueFactory(marshallingContext);

        switch (this.metaData.getReplicationConfig().getReplicationGranularity()) {
            case ATTRIBUTE: {
                @SuppressWarnings("unchecked")
                Cache<String, FineSessionCacheEntry<L>> sessionCache = (Cache<String, FineSessionCacheEntry<L>>) cache;
                @SuppressWarnings("unchecked")
                Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> attributeCache = (Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>>) cache;
                SessionAttributeMarshaller<Object, MarshalledValue<Object, MarshallingContext>> marshaller = new MarshalledValueSessionAttributeMarshaller<>(factory, marshallingContext);
                return new FineSessionFactory<L>(sessionCache, attributeCache, this.invoker, context, marshaller, localContextFactory);
            }
            case SESSION: {
                @SuppressWarnings("unchecked")
                Cache<String, CoarseSessionCacheEntry<L>> sessionCache = (Cache<String, CoarseSessionCacheEntry<L>>) cache;
                @SuppressWarnings("unchecked")
                Cache<SessionAttributesCacheKey, MarshalledValue<Map<String, Object>, MarshallingContext>> attributesCache = (Cache<SessionAttributesCacheKey, MarshalledValue<Map<String, Object>, MarshallingContext>>) cache;
                SessionAttributeMarshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> marshaller = new MarshalledValueSessionAttributeMarshaller<>(factory, marshallingContext);
                return new CoarseSessionFactory<L>(sessionCache, attributesCache, this.invoker, context, marshaller, localContextFactory);
            }
            default: {
                throw InfinispanWebMessages.MESSAGES.unknownReplicationGranularity(this.metaData.getReplicationConfig().getReplicationGranularity());
            }
        }
    }
}
