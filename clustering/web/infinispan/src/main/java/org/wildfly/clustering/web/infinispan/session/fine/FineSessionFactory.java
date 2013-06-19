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
package org.wildfly.clustering.web.infinispan.session.fine;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.MarshalledValue;
import org.jboss.as.clustering.MarshallingContext;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker.Operation;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.CacheMutator;
import org.wildfly.clustering.web.infinispan.Mutator;
import org.wildfly.clustering.web.infinispan.session.InfinispanImmutableSession;
import org.wildfly.clustering.web.infinispan.session.InfinispanSession;
import org.wildfly.clustering.web.infinispan.session.SessionAttributeMarshaller;
import org.wildfly.clustering.web.infinispan.session.SessionFactory;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionMetaData;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * {@link SessionFactory} for fine granularity sessions.
 * A given session is mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * One cache entry containing the session meta data, local context, and the set of attribute names;
 * and one cache entry per session attribute.
 * @author Paul Ferraro
 */
public class FineSessionFactory<L> implements SessionFactory<FineSessionCacheEntry<L>, L> {

    private final Cache<String, FineSessionCacheEntry<L>> sessionCache;
    private final Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> attributeCache;
    private final CacheInvoker invoker;
    private final SessionContext context;
    private final SessionAttributeMarshaller<Object, MarshalledValue<Object, MarshallingContext>> marshaller;
    private final LocalContextFactory<L> localContextFactory;

    public FineSessionFactory(Cache<String, FineSessionCacheEntry<L>> sessionCache, Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> attributeCache, CacheInvoker invoker, SessionContext context, SessionAttributeMarshaller<Object, MarshalledValue<Object, MarshallingContext>> marshaller, LocalContextFactory<L> localContextFactory) {
        this.sessionCache = sessionCache;
        this.attributeCache = attributeCache;
        this.invoker = invoker;
        this.context = context;
        this.marshaller = marshaller;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public Session<L> createSession(String id, FineSessionCacheEntry<L> entry) {
        SessionMetaData metaData = entry.getMetaData();
        Mutator mutator = metaData.isNew() ? Mutator.PASSIVE : new CacheMutator<>(this.sessionCache, this.invoker, id, entry);
        SessionAttributes attributes = new FineSessionAttributes<>(id, entry.getAttributes(), this.attributeCache, this.invoker, this.marshaller);
        return new InfinispanSession<>(id, entry.getMetaData(), attributes, entry.getLocalContext(), this.localContextFactory, this.context, mutator, this);
    }

    @Override
    public ImmutableSession createImmutableSession(String id, FineSessionCacheEntry<L> entry) {
        ImmutableSessionAttributes attributes = new FineImmutableSessionAttributes<>(id, entry.getAttributes(), this.attributeCache, this.invoker, this.marshaller);
        return new InfinispanImmutableSession(id, entry.getMetaData(), attributes, this.context);
    }

    @Override
    public FineSessionCacheEntry<L> findValue(String id) {
        return this.invoker.invoke(this.sessionCache, new FindOperation<String, FineSessionCacheEntry<L>>(id));
    }

    @Override
    public FineSessionCacheEntry<L> createValue(String id) {
        FineSessionCacheEntry<L> entry = new FineSessionCacheEntry<L>(new SimpleSessionMetaData());
        FineSessionCacheEntry<L> existing = this.invoker.invoke(this.sessionCache, new CreateOperation<>(id, entry));
        return (existing != null) ? existing : entry;
    }

    @Override
    public void remove(final String id) {
        final FineSessionCacheEntry<L> entry = this.invoker.invoke(this.sessionCache, new RemoveOperation<String, FineSessionCacheEntry<L>>(id));
        Operation<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>, Void> attributeOperation = new Operation<SessionAttributeCacheKey, MarshalledValue<Object,MarshallingContext>, Void>() {
            @Override
            public Void invoke(Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> cache) {
                for (String attribute: entry.getAttributes()) {
                    cache.remove(new SessionAttributeCacheKey(id, attribute));
                }
                return null;
            }
        };
        this.invoker.invoke(this.attributeCache, attributeOperation, Flag.IGNORE_RETURN_VALUES, Flag.SKIP_LOCKING);
    }

    @Override
    public void evict(final String id) {
        final FineSessionCacheEntry<L> entry = this.findValue(id);
        if (entry != null) {
            Operation<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>, Void> evictOperation = new Operation<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>, Void>() {
                @Override
                public Void invoke(Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> cache) {
                    for (String attribute: entry.getAttributes()) {
                        cache.getAdvancedCache().evict(new SessionAttributeCacheKey(id, attribute));
                    }
                    return null;
                }
            };
            this.invoker.invoke(this.attributeCache, evictOperation, Flag.FAIL_SILENTLY, Flag.SKIP_LOCKING);
            this.invoker.invoke(this.sessionCache, new EvictOperation<String, FineSessionCacheEntry<L>>(id), Flag.FAIL_SILENTLY);
        }
    }
}
