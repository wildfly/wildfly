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
