/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.ToIntFunction;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
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
    public AttributeDefinition getDefinition() {
        return this.definition;
    }

    @Override
    public ModelNode execute(RemoteCacheManagerMXBean manager) throws OperationFailedException {
        return new ModelNode(this.applyAsInt(manager));
    }
}
