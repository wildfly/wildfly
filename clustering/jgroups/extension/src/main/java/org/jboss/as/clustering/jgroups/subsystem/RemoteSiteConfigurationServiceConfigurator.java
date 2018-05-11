/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.jgroups.subsystem.RemoteSiteResourceDefinition.Attribute.CHANNEL;

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
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.RemoteSiteConfiguration;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class RemoteSiteConfigurationServiceConfigurator extends RemoteSiteServiceNameProvider implements ResourceServiceConfigurator, RemoteSiteConfiguration {

    private final String siteName;

    private volatile SupplierDependency<String> cluster;
    private volatile SupplierDependency<ChannelFactory> factory;

    public RemoteSiteConfigurationServiceConfigurator(PathAddress address) {
        super(address);
        this.siteName = address.getLastElement().getValue();
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<RemoteSiteConfiguration> configuration = new CompositeDependency(this.cluster, this.factory).register(builder).provides(this.getServiceName());
        Service service = Service.newInstance(configuration, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String channel = CHANNEL.resolveModelAttribute(context, model).asString();
        this.cluster = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, channel));
        this.factory = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, channel));
        return this;
    }

    @Override
    public String getName() {
        return this.siteName;
    }

    @Override
    public ChannelFactory getChannelFactory() {
        return this.factory.get();
    }

    @Override
    public String getClusterName() {
        return this.cluster.get();
    }
}
