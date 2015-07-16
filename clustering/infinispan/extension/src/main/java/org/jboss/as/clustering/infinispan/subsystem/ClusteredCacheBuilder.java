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

import static org.jboss.as.clustering.infinispan.subsystem.ClusteredCacheResourceDefinition.Attribute.ASYNC_MARSHALLING;
import static org.jboss.as.clustering.infinispan.subsystem.ClusteredCacheResourceDefinition.Attribute.QUEUE_FLUSH_INTERVAL;
import static org.jboss.as.clustering.infinispan.subsystem.ClusteredCacheResourceDefinition.Attribute.QUEUE_SIZE;
import static org.jboss.as.clustering.infinispan.subsystem.ClusteredCacheResourceDefinition.Attribute.REMOTE_TIMEOUT;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * Builds the configuration of a clustered cache.
 * @author Paul Ferraro
 */
public class ClusteredCacheBuilder extends CacheConfigurationBuilder {

    private final CacheMode mode;

    private volatile ClusteringConfiguration clustering;

    ClusteredCacheBuilder(String containerName, String cacheName, CacheMode mode) {
        super(containerName, cacheName);
        this.mode = mode;
    }

    @Override
    public Builder<Configuration> configure(ExpressionResolver resolver, ModelNode model) throws OperationFailedException {
        Mode mode = ModelNodes.asEnum(ClusteredCacheResourceDefinition.Attribute.MODE.getDefinition().resolveModelAttribute(resolver, model), Mode.class);
        ClusteringConfigurationBuilder builder = new ConfigurationBuilder().clustering().cacheMode(mode.apply(this.mode));

        if (mode.isSynchronous()) {
            builder.sync().replTimeout(REMOTE_TIMEOUT.getDefinition().resolveModelAttribute(resolver, model).asLong());
        } else {
            int queueSize = QUEUE_SIZE.getDefinition().resolveModelAttribute(resolver, model).asInt();

            builder.async()
                    .asyncMarshalling(ASYNC_MARSHALLING.getDefinition().resolveModelAttribute(resolver, model).asBoolean())
                    .useReplQueue(queueSize > 0)
                    .replQueueInterval(QUEUE_FLUSH_INTERVAL.getDefinition().resolveModelAttribute(resolver, model).asLong())
                    .replQueueMaxElements(queueSize)
            ;
        }
        this.clustering = builder.create();

        return super.configure(resolver, model);
    }

    @Override
    public ConfigurationBuilder createConfigurationBuilder() {
        ConfigurationBuilder builder = super.createConfigurationBuilder();
        builder.clustering().read(this.clustering);
        return builder;
    }
}
