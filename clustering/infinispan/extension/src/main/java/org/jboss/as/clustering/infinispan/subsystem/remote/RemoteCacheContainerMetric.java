/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.ToIntFunction;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumerates metrics for a remote cache container.
 * @author Paul Ferraro
 */
public enum RemoteCacheContainerMetric implements Metric<RemoteCacheManagerMXBean>, ToIntFunction<RemoteCacheManagerMXBean> {

    ACTIVE_CONNECTIONS("active-connections") {
        @Override
        public int applyAsInt(RemoteCacheManagerMXBean manager) {
            return manager.getActiveConnectionCount();
        }
    },
    CONNECTIONS("connections") {
        @Override
        public int applyAsInt(RemoteCacheManagerMXBean manager) {
            return manager.getConnectionCount();
        }
    },
    IDLE_CONNECTIONS("idle-connections") {
        @Override
        public int applyAsInt(RemoteCacheManagerMXBean manager) {
            return manager.getIdleConnectionCount();
        }
    }
    ;

    private final AttributeDefinition definition;

    RemoteCacheContainerMetric(String name) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.INT)
                .setFlags(AttributeAccess.Flag.GAUGE_METRIC)
                .setStorageRuntime()
                .build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }

    @Override
    public ModelNode execute(RemoteCacheManagerMXBean manager) throws OperationFailedException {
        return new ModelNode(this.applyAsInt(manager));
    }
}
