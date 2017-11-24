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

import org.infinispan.interceptors.impl.CacheLoaderInterceptor;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Enumeration of management metrics for a cache store.
 *
 * @author Paul Ferraro
 */
public enum StoreMetric implements Metric<CacheLoaderInterceptor<?, ?>> {

    CACHE_LOADER_LOADS(MetricKeys.CACHE_LOADER_LOADS, ModelType.LONG) {
        @Override
        public ModelNode execute(CacheLoaderInterceptor<?, ?> interceptor) {
            return new ModelNode(interceptor.getCacheLoaderLoads());
        }
    },
    CACHE_LOADER_MISSES(MetricKeys.CACHE_LOADER_MISSES, ModelType.LONG) {
        @Override
        public ModelNode execute(CacheLoaderInterceptor<?, ?> interceptor) {
            return new ModelNode(interceptor.getCacheLoaderMisses());
        }
    },
    ;
    private final AttributeDefinition definition;

    StoreMetric(String name, ModelType type) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type, true).setStorageRuntime().build();
    }

    @Override
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}