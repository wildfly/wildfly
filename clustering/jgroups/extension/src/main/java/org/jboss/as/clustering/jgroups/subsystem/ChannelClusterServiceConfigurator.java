/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.CLUSTER;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Builds a service providing the cluster name of a channel.
 * @author Paul Ferraro
 */
public class ChannelClusterServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator {

    private final String name;

    private volatile String cluster;

    public ChannelClusterServiceConfigurator(PathAddress address) {
        super(ChannelResourceDefinition.Capability.JCHANNEL_CLUSTER, address);
        this.name = address.getLastElement().getValue();
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.cluster = CLUSTER.resolveModelAttribute(context, model).asString(this.name);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<String> cluster = builder.provides(this.getServiceName());
        Service service = Service.newInstance(cluster, this.cluster);
        return builder.setInstance(service);
    }
}
