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

import static org.jboss.as.clustering.infinispan.subsystem.BackupForResourceDefinition.Attribute.CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.BackupForResourceDefinition.Attribute.SITE;

import org.infinispan.configuration.cache.BackupForConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a service that provides the {@link BackupForConfiguration} for a cache.
 * @author Paul Ferraro
 */
public class BackupForBuilder extends CacheComponentBuilder<BackupForConfiguration> implements ResourceServiceBuilder<BackupForConfiguration> {

    private volatile org.infinispan.configuration.cache.BackupForBuilder builder = new ConfigurationBuilder().sites().backupFor();

    BackupForBuilder(String containerName, String cacheName) {
        super(CacheComponent.BACKUP_FOR, containerName, cacheName);
    }

    @Override
    public Builder<BackupForConfiguration> configure(ExpressionResolver resolver, ModelNode model) throws OperationFailedException {
        String site = ModelNodes.asString(SITE.getDefinition().resolveModelAttribute(resolver, model));
        if (site != null) {
            this.builder.remoteSite(site).remoteCache(CACHE.getDefinition().resolveModelAttribute(resolver, model).asString());
        }
        return this;
    }

    @Override
    public BackupForConfiguration getValue() {
        return this.builder.create();
    }
}
