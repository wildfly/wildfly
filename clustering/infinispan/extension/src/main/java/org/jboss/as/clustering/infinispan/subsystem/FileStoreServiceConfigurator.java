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

import org.infinispan.configuration.cache.SingleFileStoreConfiguration;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class FileStoreServiceConfigurator extends StoreServiceConfigurator<SingleFileStoreConfiguration, SingleFileStoreConfigurationBuilder> {

    private final String containerName;

    private volatile SupplierDependency<PathManager> pathManager;
    private volatile String relativePath;
    private volatile String relativeTo;

    FileStoreServiceConfigurator(PathAddress address) {
        super(address, SingleFileStoreConfigurationBuilder.class);
        this.containerName = address.getParent().getParent().getLastElement().getValue();
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.pathManager.register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.pathManager = new ServiceSupplierDependency<>(CommonRequirement.PATH_MANAGER.getServiceName(context));
        this.relativePath = RELATIVE_PATH.resolveModelAttribute(context, model).asString(InfinispanExtension.SUBSYSTEM_NAME + File.separatorChar + this.containerName);
        this.relativeTo = RELATIVE_TO.resolveModelAttribute(context, model).asString();
        return super.configure(context, model);
    }

    @Override
    public void accept(SingleFileStoreConfigurationBuilder builder) {
        builder.location(this.pathManager.get().resolveRelativePathEntry(this.relativePath, this.relativeTo));
    }
}
