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

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.SiteConfiguration;
import org.infinispan.configuration.global.SiteConfigurationBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;

/**
 * @author Paul Ferraro
 */
public class SiteBuilder extends CacheContainerComponentBuilder<SiteConfiguration> {

    private final InjectedValue<ChannelFactory> factory = new InjectedValue<>();

    private volatile String channelName = null;

    public SiteBuilder(String containerName) {
        super(CacheContainerComponent.SITE, containerName);
    }

    @Override
    public ServiceBuilder<SiteConfiguration> build(ServiceTarget target) {
        ServiceBuilder<SiteConfiguration> builder = super.build(target);
        if (this.channelName != null) {
            builder.addDependency(ChannelServiceName.FACTORY.getServiceName(this.channelName), ChannelFactory.class, this.factory);
        }
        return builder;
    }

    @Override
    public SiteConfiguration getValue() {
        SiteConfigurationBuilder builder = new GlobalConfigurationBuilder().site();
        ChannelFactory factory = this.factory.getOptionalValue();
        if (factory != null) {
            RelayConfiguration relay = this.factory.getValue().getProtocolStackConfiguration().getRelay();
            if (relay != null) {
                builder.localSite(relay.getSiteName());
            }
        }
        return builder.create();
    }

    SiteBuilder setChannelName(String channelName) {
        this.channelName = channelName;
        return this;
    }
}
