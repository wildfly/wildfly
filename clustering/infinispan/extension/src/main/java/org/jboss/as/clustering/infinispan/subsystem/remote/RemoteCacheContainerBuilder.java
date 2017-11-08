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
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.RemoteCacheContainer;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Radoslav Husar
 */
public class RemoteCacheContainerBuilder implements ResourceServiceBuilder<RemoteCacheContainer> {

    private ValueDependency<Configuration> configuration;
    private final PathAddress address;
    private final String name;

    public RemoteCacheContainerBuilder(PathAddress address) {
        this.address = address;
        this.name = address.getLastElement().getValue();
    }

    @Override
    public ServiceName getServiceName() {
        return RemoteCacheContainerResourceDefinition.Capability.CONTAINER.getServiceName(this.address);
    }

    @Override
    public RemoteCacheContainerBuilder configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.configuration = new InjectedValueDependency<>(InfinispanRequirement.REMOTE_CONTAINER_CONFIGURATION.getServiceName(context, this.name), Configuration.class);
        return this;
    }

    @Override
    public ServiceBuilder<RemoteCacheContainer> build(ServiceTarget target) {
        Function<RemoteCacheManager, RemoteCacheContainer> mapper = remoteCacheManager -> new ManagedRemoteCacheContainer(this.name, remoteCacheManager);

        Supplier<RemoteCacheManager> supplier = () -> {
            Configuration configuration = this.configuration.getValue();
            RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);
            remoteCacheManager.start();
            InfinispanLogger.ROOT_LOGGER.debugf("%s remote cache container started", this.name);
            return remoteCacheManager;
        };

        Consumer<RemoteCacheManager> destroyer = remoteCacheManager -> {
            remoteCacheManager.stop();
            InfinispanLogger.ROOT_LOGGER.debugf("%s remote cache container stopped", this.name);
        };

        Service<RemoteCacheContainer> service = new SuppliedValueService<>(mapper, supplier, destroyer);
        ServiceBuilder<RemoteCacheContainer> builder = target.addService(this.getServiceName(), service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                ;
        return this.configuration.register(builder);
    }
}
