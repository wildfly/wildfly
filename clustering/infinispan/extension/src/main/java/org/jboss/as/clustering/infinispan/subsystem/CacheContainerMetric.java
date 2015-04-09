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

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Enumeration of management metrics for a cache container.
 * @author Paul Ferraro
 */
public enum CacheContainerMetric implements Metric<EmbeddedCacheManager> {

    CACHE_MANAGER_STATUS(MetricKeys.CACHE_MANAGER_STATUS, ModelType.STRING) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            return new ModelNode(manager.getStatus().toString());
        }
    },
    CLUSTER_NAME(MetricKeys.CLUSTER_NAME, ModelType.STRING) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            String clusterName = manager.getClusterName();
            return (clusterName != null) ? new ModelNode(clusterName) : null;
        }
    },
    COORDINATOR_ADDRESS(MetricKeys.COORDINATOR_ADDRESS, ModelType.STRING) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            Address address = manager.getCoordinator();
            return (address != null) ? new ModelNode(address.toString()) : null;
        }
    },
    IS_COORDINATOR(MetricKeys.IS_COORDINATOR, ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            return new ModelNode(manager.isCoordinator());
        }
    },
    LOCAL_ADDRESS(MetricKeys.LOCAL_ADDRESS, ModelType.STRING) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            Address address = manager.getAddress();
            return (address != null) ? new ModelNode(address.toString()) : null;
        }
    },
    ;
    private final AttributeDefinition definition;

    CacheContainerMetric(String name, ModelType type) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type, true).setStorageRuntime().build();
    }

    @Override
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}