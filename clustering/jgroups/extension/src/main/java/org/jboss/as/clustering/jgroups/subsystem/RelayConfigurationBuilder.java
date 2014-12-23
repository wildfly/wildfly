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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.RemoteSiteConfiguration;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class RelayConfigurationBuilder extends AbstractProtocolConfigurationBuilder<RelayConfiguration> implements RelayConfiguration {

    private final List<ValueDependency<RemoteSiteConfiguration>> sites = new LinkedList<>();
    private String siteName = null;

    public RelayConfigurationBuilder(String stackName) {
        super(stackName, RelayConfiguration.PROTOCOL_NAME);
    }

    @Override
    public ServiceName getServiceName() {
        return ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(this.stackName).append("relay");
    }

    @Override
    public ServiceBuilder<RelayConfiguration> build(ServiceTarget target) {
        ServiceBuilder<RelayConfiguration> builder = super.build(target);
        for (Dependency site : this.sites) {
            site.register(builder);
        }
        return builder;
    }

    @Override
    public RelayConfiguration getValue() {
        return this;
    }

    public RelayConfigurationBuilder setSiteName(String siteName) {
        this.siteName = siteName;
        return this;
    }

    public RemoteSiteConfigurationBuilder addRemoteSite(String name, String channel) {
        RemoteSiteConfigurationBuilder builder = new RemoteSiteConfigurationBuilder(this.stackName, name).setChannel(channel);
        this.sites.add(new InjectedValueDependency<>(builder, RemoteSiteConfiguration.class));
        return builder;
    }

    @Override
    public RelayConfigurationBuilder setModule(ModuleIdentifier module) {
        super.setModule(module);
        return this;
    }

    @Override
    public RelayConfigurationBuilder setSocketBinding(String socketBindingName) {
        super.setSocketBinding(socketBindingName);
        return this;
    }

    @Override
    public RelayConfigurationBuilder addProperty(String name, String value) {
        super.addProperty(name, value);
        return this;
    }

    @Override
    public String getSiteName() {
        return this.siteName;
    }

    @Override
    public List<RemoteSiteConfiguration> getRemoteSites() {
        List<RemoteSiteConfiguration> sites = new ArrayList<>(this.sites.size());
        for (ValueDependency<RemoteSiteConfiguration> site : this.sites) {
            sites.add(site.getValue());
        }
        return sites;
    }
}
