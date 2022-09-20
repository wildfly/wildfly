/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
