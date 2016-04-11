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

package org.wildfly.clustering.web.infinispan.session.fine;

import java.util.Map;
import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.marshalling.jboss.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.jboss.MarshalledValue;
import org.wildfly.clustering.marshalling.jboss.Marshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.SessionAttributesFactory;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.SessionAttributes;

/**
 * {@link SessionAttributesFactory} for fine granularity sessions.
 * A given session's attributes are mapped to N co-located cache entries, where N is the number of session attributes.
 * @author Paul Ferraro
 */
public class FineSessionAttributesFactory implements SessionAttributesFactory<Object> {

    private static final Object VALUE = new Object();

    private final Cache<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>> cache;
    private final Marshaller<Object, MarshalledValue<Object, MarshallingContext>, MarshallingContext> marshaller;
    private final Predicate<Map.Entry<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>>> invalidAttribute;
    private final CacheProperties properties;

    public FineSessionAttributesFactory(Cache<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>> cache, Marshaller<Object, MarshalledValue<Object, MarshallingContext>, MarshallingContext> marshaller, CacheProperties properties) {
        this.cache = cache;
        this.marshaller = marshaller;
        this.properties = properties;
        this.invalidAttribute = entry -> {
            try {
                this.marshaller.read(entry.getValue());
                return false;
            } catch (InvalidSerializedFormException e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, entry.getKey().getValue(), entry.getKey().getAttribute());
                return true;
            }
        };
    }

    @Override
    public Object createValue(String id, Void context) {
        // Preemptively read all attributes to detect invalid session attributes
        if (this.cache.getAdvancedCache().getGroup(id).entrySet().stream().filter(entry -> ((Map.Entry<?, ?>) entry).getKey() instanceof SessionAttributeKey).anyMatch(this.invalidAttribute)) {
            // If any attributes are invalid - remove them all
            this.remove(id);
        }
        return VALUE;
    }

    @Override
    public Object findValue(String id) {
        // Preemptively read all attributes to detect invalid session attributes
        if (this.cache.getAdvancedCache().getGroup(id).entrySet().stream().filter(entry -> ((Map.Entry<?, ?>) entry).getKey() instanceof SessionAttributeKey).anyMatch(this.invalidAttribute)) {
            // Invalidate
            this.remove(id);
            return null;
        }
        return VALUE;
    }

    @Override
    public boolean remove(String id) {
        this.cache.getAdvancedCache().removeGroup(id);
        return true;
    }

    @Override
    public void evict(String id) {
        this.cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).getGroup(id).keySet().stream().filter((Object key) -> key instanceof SessionAttributeKey).forEach(key -> {
            try {
                this.cache.evict(key);
            } catch (Throwable e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToPassivateSessionAttribute(e, id, key.getAttribute());
            }
        });
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Object value) {
        return new FineSessionAttributes<>(id, this.cache, this.marshaller, this.properties);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, Object value) {
        return new FineImmutableSessionAttributes<>(id, this.cache, this.marshaller);
    }
}
