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

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.RemoteSiteConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class RemoteSiteConfigurationBuilder implements ResourceServiceBuilder<RemoteSiteConfiguration>, RemoteSiteConfiguration {

    private final ServiceNameProvider relayProvider;
    private final String siteName;

    private volatile ValueDependency<String> cluster;
    private volatile ValueDependency<ChannelFactory> factory;

    public RemoteSiteConfigurationBuilder(PathAddress relayAddress, String siteName) {
        this.relayProvider = new ProtocolServiceNameProvider(relayAddress);
        this.siteName = siteName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.relayProvider.getServiceName().append(this.siteName);
    }

    @Override
    public ServiceBuilder<RemoteSiteConfiguration> build(ServiceTarget target) {
        Value<RemoteSiteConfiguration> value = new ImmediateValue<>(this);
        ServiceBuilder<RemoteSiteConfiguration> builder = target.addService(this.getServiceName(), new ValueService<>(value)).setInitialMode(ServiceController.Mode.ON_DEMAND);
        Stream.of(this.cluster, this.factory).forEach(dependency -> dependency.register(builder));
        return builder;
    }

    @Override
    public Builder<RemoteSiteConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String channel = CHANNEL.resolveModelAttribute(context, model).asString();
        this.cluster = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, channel), String.class);
        this.factory = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, channel), ChannelFactory.class);
        return this;
    }

    @Override
    public String getName() {
        return this.siteName;
    }

    @Override
    public ChannelFactory getChannelFactory() {
        return this.factory.getValue();
    }

    @Override
    public String getClusterName() {
        return this.cluster.getValue();
    }
}
