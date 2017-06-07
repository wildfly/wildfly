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
import org.infinispan.configuration.cache.PartitionHandlingConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a service providing a {@link PartitionHandlingConfiguration}.
 * @author Paul Ferraro
 */
public class PartitionHandlingBuilder extends ComponentBuilder<PartitionHandlingConfiguration> {

    private final PartitionHandlingConfigurationBuilder builder = new ConfigurationBuilder().clustering().partitionHandling();

    PartitionHandlingBuilder(PathAddress cacheAddress) {
        super(CacheComponent.PARTITION_HANDLING, cacheAddress);
    }

    @Override
    public PartitionHandlingConfiguration getValue() {
        return this.builder.create();
    }

    @Override
    public Builder<PartitionHandlingConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.builder.enabled(ENABLED.resolveModelAttribute(context, model).asBoolean());
        return this;
    }
}
