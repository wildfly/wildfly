/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.RemoteCacheContainer;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Radoslav Husar
 */
public class RemoteCacheContainerServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Function<RemoteCacheManager, RemoteCacheContainer>, Supplier<RemoteCacheManager>, Consumer<RemoteCacheManager> {

    private final String name;

    private volatile SupplierDependency<Configuration> configuration;

    public RemoteCacheContainerServiceConfigurator(PathAddress address) {
        super(RemoteCacheContainerResourceDefinition.Capability.CONTAINER, address);
        this.name = address.getLastElement().getValue();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.configuration = new ServiceSupplierDependency<>(InfinispanRequirement.REMOTE_CONTAINER_CONFIGURATION.getServiceName(context, this.name));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        Consumer<RemoteCacheContainer> container = this.configuration.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(container, this, this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public RemoteCacheManager get() {
        Configuration configuration = this.configuration.get();
        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);
        remoteCacheManager.start();
        InfinispanLogger.ROOT_LOGGER.remoteCacheContainerStarted(this.name);
        return remoteCacheManager;
    }

    @Override
    public void accept(RemoteCacheManager remoteCacheManager) {
        remoteCacheManager.stop();
        InfinispanLogger.ROOT_LOGGER.remoteCacheContainerStopped(this.name);
    }

    @Override
    public RemoteCacheContainer apply(RemoteCacheManager remoteCacheManager) {
        return new ManagedRemoteCacheContainer(RemoteCacheContainerServiceConfigurator.this.name, remoteCacheManager);
    }
}
