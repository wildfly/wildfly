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

import org.infinispan.util.concurrent.locks.impl.DefaultLockManager;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Enumeration of locking management metrics for a cache.
 *
 * @author Paul Ferraro
 */
public enum LockingMetric implements Metric<DefaultLockManager> {

    CURRENT_CONCURRENCY_LEVEL(MetricKeys.CURRENT_CONCURRENCY_LEVEL, ModelType.INT) {
        @Override
        public ModelNode execute(DefaultLockManager manager) {
            return new ModelNode(manager.getConcurrencyLevel());
        }
    },
    NUMBER_OF_LOCKS_AVAILABLE(MetricKeys.NUMBER_OF_LOCKS_AVAILABLE, ModelType.INT) {
        @Override
        public ModelNode execute(DefaultLockManager manager) {
            return new ModelNode(manager.getNumberOfLocksAvailable());
        }
    },
    NUMBER_OF_LOCKS_HELD(MetricKeys.NUMBER_OF_LOCKS_HELD, ModelType.INT) {
        @Override
        public ModelNode execute(DefaultLockManager manager) {
            return new ModelNode(manager.getNumberOfLocksHeld());
        }
    },
    ;
    private final AttributeDefinition definition;

    LockingMetric(String name, ModelType type) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type, true).setStorageRuntime().build();
    }

    @Override
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}
