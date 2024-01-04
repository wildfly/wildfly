/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import static org.wildfly.extension.clustering.ejb.BeanManagementResourceDefinition.Attribute.MAX_ACTIVE_BEANS;

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
import org.wildfly.clustering.ejb.bean.BeanManagementConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.bean.BeanDeploymentMarshallingContext;
import org.wildfly.clustering.ejb.cache.bean.BeanMarshallerFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Common service configurator for bean management services.
 * @author Paul Ferraro
 */
public abstract class BeanManagementServiceConfigurator  extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<BeanManagementProvider>, Function<String, BeanManagementProvider>, BeanManagementConfiguration {

    private final String name;
    private volatile Integer maxActiveBeans;

    public BeanManagementServiceConfigurator(PathAddress address) {
        super(BeanManagementResourceDefinition.Capability.BEAN_MANAGEMENT_PROVIDER, address);
        // set the name of this provider
        this.name = address.getLastElement().getValue();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
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
        return this.apply(this.name);
    }

    @Override
    public Integer getMaxActiveBeans() {
        return this.maxActiveBeans;
    }

    @Override
    public Function<BeanDeploymentMarshallingContext, ByteBufferMarshaller> getMarshallerFactory() {
        return BeanMarshallerFactory.JBOSS;
    }
}
