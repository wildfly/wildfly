/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;

/**
 * Enumerates the supported cache types.
 * @author Paul Ferraro
 */
enum CacheType {
    LOCAL(ModelKeys.LOCAL_CACHE, new LocalCacheAddHandler(), new CacheRemoveHandler()),
    DISTRIBUTED(ModelKeys.DISTRIBUTED_CACHE, new DistributedCacheAddHandler(), LOCAL.getRemoveHandler()),
    REPLICATED(ModelKeys.REPLICATED_CACHE, new ReplicatedCacheAddHandler(), LOCAL.getRemoveHandler()),
    INVALIDATION(ModelKeys.INVALIDATION_CACHE, new InvalidationCacheAddHandler(), LOCAL.getRemoveHandler()),
    ;

    private static final Map<String, CacheType> TYPES = new HashMap<>();
    static {
        for (CacheType type: values()) {
            TYPES.put(type.key, type);
        }
    }

    static CacheType forName(String key) {
        return TYPES.get(key);
    }

    private final String key;
    private final CacheAddHandler addHandler;
    private final CacheRemoveHandler removeHandler;

    private CacheType(String key, CacheAddHandler addHandler, CacheRemoveHandler removeHandler) {
        this.key = key;
        this.addHandler = addHandler;
        this.removeHandler = removeHandler;
    }

    public ResourceDescriptionResolver getResourceDescriptionResolver() {
        return new InfinispanResourceDescriptionResolver(this.key);
    }

    public PathElement pathElement() {
        return pathElement(PathElement.WILDCARD_VALUE);
    }

    public PathElement pathElement(String name) {
        return PathElement.pathElement(this.key, name);
    }

    public CacheAddHandler getAddHandler() {
        return this.addHandler;
    }

    public CacheRemoveHandler getRemoveHandler() {
        return this.removeHandler;
    }

    public boolean hasSharedState() {
        return EnumSet.of(REPLICATED, DISTRIBUTED).contains(this);
    }
}
