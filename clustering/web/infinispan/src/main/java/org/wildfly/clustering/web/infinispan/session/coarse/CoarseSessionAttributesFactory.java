/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.session.coarse;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.infinispan.Mutator;
import org.wildfly.clustering.marshalling.jboss.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.jboss.MarshalledValue;
import org.wildfly.clustering.marshalling.jboss.Marshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.SessionAttributes;
import org.wildfly.clustering.web.infinispan.session.SessionAttributesFactory;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * {@link SessionAttributesFactory} for coarse granularity sessions, where all session attributes are stored in a single cache entry.
 * @author Paul Ferraro
 */
public class CoarseSessionAttributesFactory implements SessionAttributesFactory<Map.Entry<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>>> {

    private final Cache<SessionAttributesKey, MarshalledValue<Map<String, Object>, MarshallingContext>> cache;
    private final Marshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>, MarshallingContext> marshaller;
    private final CacheProperties properties;

    public CoarseSessionAttributesFactory(Cache<SessionAttributesKey, MarshalledValue<Map<String, Object>, MarshallingContext>> cache, Marshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>, MarshallingContext> marshaller, CacheProperties properties) {
        this.cache = cache;
        this.marshaller = marshaller;
        this.properties = properties;
    }

    @Override
    public Map.Entry<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> createValue(String id, Void context) {
        Map<String, Object> attributes = this.properties.isLockOnRead() ? new HashMap<>() : new ConcurrentHashMap<>();
        MarshalledValue<Map<String, Object>, MarshallingContext> value = this.marshaller.write(attributes);
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(new SessionAttributesKey(id), value);
        return new SimpleImmutableEntry<>(attributes, value);
    }

    @Override
    public Map.Entry<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> findValue(String id) {
        MarshalledValue<Map<String, Object>, MarshallingContext> value = this.cache.get(new SessionAttributesKey(id));
        if (value != null) {
            try {
                Map<String, Object> attributes = this.marshaller.read(value);
                return new SimpleImmutableEntry<>(attributes, value);
            } catch (InvalidSerializedFormException e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToActivateSession(e, id);
                this.remove(id);
            }
        }
        return null;
    }

    @Override
    public boolean remove(String id) {
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new SessionAttributesKey(id));
        return true;
    }

    @Override
    public void evict(String id) {
        this.cache.evict(new SessionAttributesKey(id));
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Map.Entry<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> entry) {
        SessionAttributesKey key = new SessionAttributesKey(id);
        Mutator mutator = this.properties.isTransactional() && this.cache.getAdvancedCache().getCacheEntry(key).isCreated() ? Mutator.PASSIVE : new CacheEntryMutator<>(this.cache, key, entry.getValue());
        return new CoarseSessionAttributes(entry.getKey(), mutator, this.marshaller.getContext(), this.properties);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, Map.Entry<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> entry) {
        return new CoarseImmutableSessionAttributes(entry.getKey());
    }
}
