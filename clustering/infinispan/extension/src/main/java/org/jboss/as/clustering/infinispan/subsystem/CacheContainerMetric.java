/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumeration of management metrics for a cache container.
 * @author Paul Ferraro
 */
public enum CacheContainerMetric implements Metric<EmbeddedCacheManager> {

    CACHE_MANAGER_STATUS("cache-manager-status", ModelType.STRING) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            return new ModelNode(manager.getStatus().toString());
        }
    },
    CLUSTER_NAME("cluster-name", ModelType.STRING) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            String clusterName = manager.getClusterName();
            return (clusterName != null) ? new ModelNode(clusterName) : null;
        }
    },
    COORDINATOR_ADDRESS("coordinator-address", ModelType.STRING) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            Address address = manager.getCoordinator();
            return (address != null) ? new ModelNode(address.toString()) : null;
        }
    },
    IS_COORDINATOR("is-coordinator", ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            return new ModelNode(manager.isCoordinator());
        }
    },
    LOCAL_ADDRESS("local-address", ModelType.STRING) {
        @Override
        public ModelNode execute(EmbeddedCacheManager manager) {
            Address address = manager.getAddress();
            return (address != null) ? new ModelNode(address.toString()) : null;
        }
    },
    ;
    private final AttributeDefinition definition;

    CacheContainerMetric(String name, ModelType type) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type, true)
                .setFlags(AttributeAccess.Flag.GAUGE_METRIC)
                .setStorageRuntime()
                .build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }
}