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
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.marshalling.MarshalledValue;
import org.jboss.as.clustering.marshalling.MarshalledValueFactory;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.as.clustering.marshalling.SimpleMarshalledValueFactory;
import org.jboss.as.clustering.marshalling.SimpleMarshallingContextFactory;
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
    private final Module module;
    private final JBossWebMetaData metaData;
    private final CacheInvoker invoker = new RetryingCacheInvoker(10, 100);
    private final Value<Cache> cache;
    private final Value<KeyAffinityServiceFactory> affinityFactory;

    public InfinispanSessionManagerFactory(Module module, JBossWebMetaData metaData, Value<Cache> cache, Value<KeyAffinityServiceFactory> affinityFactory) {
        this.module = module;
        this.cache = cache;
        this.affinityFactory = affinityFactory;
        this.metaData = metaData;
    }

    @Override
    public SessionManagerFactory getValue() {
        return this;
    }

    @Override
    public <L> SessionManager<L> createSessionManager(SessionContext context, SessionIdentifierFactory identifierFactory, LocalContextFactory<L> localContextFactory) {
        return new InfinispanSessionManager<>(context, identifierFactory, this.cache.getValue(), this.<L>getSessionFactory(context, localContextFactory), this.affinityFactory.getValue(), this.metaData);
    }

    private <L> SessionFactory<?, L> getSessionFactory(SessionContext context, LocalContextFactory<L> localContextFactory) {
        MarshallingContext marshallingContext = new SimpleMarshallingContextFactory().createMarshallingContext(new SessionAttributeMarshallingContext(this.module), this.module.getClassLoader());
        MarshalledValueFactory<MarshallingContext> factory = new SimpleMarshalledValueFactory(marshallingContext);

        switch (this.metaData.getReplicationConfig().getReplicationGranularity()) {
            case ATTRIBUTE: {
                Cache<String, FineSessionCacheEntry<L>> sessionCache = this.cache.getValue();
                Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> attributeCache = this.cache.getValue();
                SessionAttributeMarshaller<Object, MarshalledValue<Object, MarshallingContext>> marshaller = new MarshalledValueSessionAttributeMarshaller<>(factory, marshallingContext);
                return new FineSessionFactory<>(sessionCache, attributeCache, this.invoker, context, marshaller, localContextFactory);
            }
            case SESSION: {
                Cache<String, CoarseSessionCacheEntry<L>> sessionCache = this.cache.getValue();
                Cache<SessionAttributesCacheKey, MarshalledValue<Map<String, Object>, MarshallingContext>> attributesCache = this.cache.getValue();
                SessionAttributeMarshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> marshaller = new MarshalledValueSessionAttributeMarshaller<>(factory, marshallingContext);
                return new CoarseSessionFactory<>(sessionCache, attributesCache, this.invoker, context, marshaller, localContextFactory);
            }
            default: {
                throw InfinispanWebMessages.MESSAGES.unknownReplicationGranularity(this.metaData.getReplicationConfig().getReplicationGranularity());
            }
        }
    }
}
