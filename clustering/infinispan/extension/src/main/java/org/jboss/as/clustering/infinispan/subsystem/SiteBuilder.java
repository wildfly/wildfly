/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.JGroupsTransportResourceDefinition.Attribute.CHANNEL;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.SiteConfiguration;
import org.infinispan.configuration.global.SiteConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class SiteBuilder extends GlobalComponentBuilder<SiteConfiguration> {

    private volatile ValueDependency<ChannelFactory> factory;

    public SiteBuilder(PathAddress address) {
        super(CacheContainerComponent.SITE, address);
    }

    @Override
    public ServiceBuilder<SiteConfiguration> build(ServiceTarget target) {
        ServiceBuilder<SiteConfiguration> builder = super.build(target);
        return (this.factory != null) ? this.factory.register(builder) : builder;
    }

    @Override
    public Builder<SiteConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String channel = CHANNEL.resolveModelAttribute(context, model).asStringOrNull();
        this.factory = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, channel), ChannelFactory.class);
        return this;
    }

    @Override
    public SiteConfiguration getValue() {
        SiteConfigurationBuilder builder = new GlobalConfigurationBuilder().site();
        if (this.factory != null) {
            RelayConfiguration relay = this.factory.getValue().getProtocolStackConfiguration().getRelay().orElse(null);
            if (relay != null) {
                builder.localSite(relay.getSiteName());
            }
        }
        return builder.create();
    }
}
