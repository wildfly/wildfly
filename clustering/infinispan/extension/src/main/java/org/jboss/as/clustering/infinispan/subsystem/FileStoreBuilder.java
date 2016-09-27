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

import static org.jboss.as.clustering.infinispan.subsystem.FileStoreResourceDefinition.Attribute.RELATIVE_PATH;
import static org.jboss.as.clustering.infinispan.subsystem.FileStoreResourceDefinition.Attribute.RELATIVE_TO;

import java.io.File;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class FileStoreBuilder extends StoreBuilder {

    private final InjectedValue<PathManager> pathManager = new InjectedValue<>();
    private final String containerName;

    private volatile SingleFileStoreConfigurationBuilder builder;
    private volatile String relativePath;
    private volatile String relativeTo;

    FileStoreBuilder(PathAddress cacheAddress) {
        super(cacheAddress);
        this.containerName = cacheAddress.getParent().getLastElement().getValue();
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return super.build(target).addDependency(PathManagerService.SERVICE_NAME, PathManager.class, this.pathManager);
    }

    @Override
    public PersistenceConfiguration getValue() {
        this.builder.location(this.pathManager.getValue().resolveRelativePathEntry(this.relativePath, this.relativeTo));
        return super.getValue();
    }

    @Override
    StoreConfigurationBuilder<?, ?> createStore(OperationContext context, ModelNode model) throws OperationFailedException {
        this.relativePath = ModelNodes.optionalString(RELATIVE_PATH.resolveModelAttribute(context, model)).orElse(InfinispanExtension.SUBSYSTEM_NAME + File.separatorChar + this.containerName);
        this.relativeTo = RELATIVE_TO.resolveModelAttribute(context, model).asString();
        this.builder = new ConfigurationBuilder().persistence().addSingleFileStore();
        return this.builder;
    }
}
