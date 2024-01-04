/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class ThreadPoolFactoryServiceConfigurator extends ThreadPoolServiceNameProvider implements ResourceServiceConfigurator, ThreadPoolConfiguration {

    private final ThreadPoolDefinition definition;

    private volatile int minThreads = 0;
    private volatile int maxThreads = Integer.MAX_VALUE;
    private volatile long keepAliveTime = 0;

    public ThreadPoolFactoryServiceConfigurator(ThreadPoolDefinition definition, PathAddress address) {
        super(address);
        this.definition = definition;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.minThreads = this.definition.getMinThreads().resolveModelAttribute(context, model).asInt();
        this.maxThreads = this.definition.getMaxThreads().resolveModelAttribute(context, model).asInt();
        this.keepAliveTime = this.definition.getKeepAliveTime().resolveModelAttribute(context, model).asLong();
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<ThreadPoolConfiguration> factory = builder.provides(this.getServiceName());
        Service service = Service.newInstance(factory, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public int getMinThreads() {
        return this.minThreads;
    }

    @Override
    public int getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public long getKeepAliveTime() {
        return this.keepAliveTime;
    }
}
