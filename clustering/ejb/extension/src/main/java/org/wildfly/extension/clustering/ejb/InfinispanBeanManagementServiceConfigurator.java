/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.ejb;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ejb.BeanManagementProvider;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.ejb.infinispan.InfinispanBeanManagementProvider;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;

import static org.wildfly.extension.clustering.ejb.InfinispanBeanManagementResourceDefinition.Attribute.CACHE;
import static org.wildfly.extension.clustering.ejb.InfinispanBeanManagementResourceDefinition.Attribute.CACHE_CONTAINER;
import static org.wildfly.extension.clustering.ejb.InfinispanBeanManagementResourceDefinition.Attribute.MAX_ACTIVE_BEANS;

/**
 * Service configurator for Infinispan bean management providers.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanBeanManagementServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<BeanManagementProvider>, BeanManagerFactoryServiceConfiguratorConfiguration {

    private volatile String name;
    private volatile String containerName;
    private volatile String cacheName;
    private volatile Integer maxActiveBeans;

    public InfinispanBeanManagementServiceConfigurator(PathAddress address) {
        super(InfinispanBeanManagementResourceDefinition.Capability.BEAN_MANAGEMENT_PROVIDER, address);
        // set the name of this provider
        this.name = address.getLastElement().getValue();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        this.maxActiveBeans = MAX_ACTIVE_BEANS.resolveModelAttribute(context, model).asIntOrNull();
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<BeanManagementProvider> provider = builder.provides(name);
        Service service = new FunctionalService<>(provider, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public BeanManagementProvider get() {
        return new InfinispanBeanManagementProvider<>(this.name, this);
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }

    @Override
    public Integer getMaxActiveBeans() {
        return this.maxActiveBeans;
    }
}
