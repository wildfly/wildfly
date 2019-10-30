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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.PartitionHandlingResourceDefinition.Attribute.*;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.partitionhandling.PartitionHandling;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Builds a service providing a {@link PartitionHandlingConfiguration}.
 * @author Paul Ferraro
 */
public class PartitionHandlingServiceConfigurator extends ComponentServiceConfigurator<PartitionHandlingConfiguration> {

    private volatile boolean enabled;

    PartitionHandlingServiceConfigurator(PathAddress address) {
        super(CacheComponent.PARTITION_HANDLING, address);
    }

    @Override
    public PartitionHandlingConfiguration get() {
        return new ConfigurationBuilder().clustering().partitionHandling()
                .whenSplit(this.enabled ? PartitionHandling.DENY_READ_WRITES : PartitionHandling.ALLOW_READ_WRITES)
                .mergePolicy(MergePolicy.NONE)
                .create();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.enabled = ENABLED.resolveModelAttribute(context, model).asBoolean();
        return this;
    }
}
