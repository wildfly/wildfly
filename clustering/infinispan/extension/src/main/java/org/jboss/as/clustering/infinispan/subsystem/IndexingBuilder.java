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

import static org.jboss.as.clustering.infinispan.subsystem.IndexingResourceDefinition.Attribute.INDEX;
import static org.jboss.as.clustering.infinispan.subsystem.IndexingResourceDefinition.Attribute.PROPERTIES;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.cache.IndexingConfiguration;
import org.infinispan.configuration.cache.IndexingConfigurationBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class IndexingBuilder extends CacheComponentBuilder<IndexingConfiguration> implements ResourceServiceBuilder<IndexingConfiguration> {

    private final IndexingConfigurationBuilder builder = new ConfigurationBuilder().indexing();

    IndexingBuilder(String containerName, String cacheName) {
        super(CacheComponent.INDEXING, containerName, cacheName);
    }

    @Override
    public Builder<IndexingConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.builder.index(ModelNodes.asEnum(INDEX.getDefinition().resolveModelAttribute(context, model), Index.class));
        this.builder.withProperties(ModelNodes.asProperties(PROPERTIES.getDefinition().resolveModelAttribute(context, model)));
        return this;
    }

    @Override
    public IndexingConfiguration getValue() {
        return this.builder.create();
    }
}
