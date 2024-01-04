/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import static org.wildfly.extension.clustering.ejb.InfinispanTimerManagementResourceDefinition.Attribute.*;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ejb.infinispan.timer.InfinispanTimerManagementConfiguration;
import org.wildfly.clustering.ejb.infinispan.timer.InfinispanTimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerManagementServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, InfinispanTimerManagementConfiguration {

    private volatile String containerName;
    private volatile String cacheName;
    private volatile Integer maxActiveTimers;
    private volatile Function<Module, ByteBufferMarshaller> marshallerFactory;

    public InfinispanTimerManagementServiceConfigurator(PathAddress address) {
        super(InfinispanTimerManagementResourceDefinition.Capability.TIMER_MANAGEMENT_PROVIDER, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        this.maxActiveTimers = MAX_ACTIVE_TIMERS.resolveModelAttribute(context, model).asIntOrNull();
        this.marshallerFactory = TimerContextMarshallerFactory.valueOf(MARSHALLER.resolveModelAttribute(context, model).asString());
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<TimerManagementProvider> provider = builder.provides(name);
        return builder.setInstance(Service.newInstance(provider, new InfinispanTimerManagementProvider(this))).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Function<Module, ByteBufferMarshaller> getMarshallerFactory() {
        return this.marshallerFactory;
    }

    @Override
    public Integer getMaxActiveTimers() {
        return this.maxActiveTimers;
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }
}
