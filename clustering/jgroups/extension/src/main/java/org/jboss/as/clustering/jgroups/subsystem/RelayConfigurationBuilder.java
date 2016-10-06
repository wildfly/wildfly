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

import static org.jboss.as.clustering.jgroups.subsystem.RelayResourceDefinition.Attribute.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.RemoteSiteConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class RelayConfigurationBuilder extends AbstractProtocolConfigurationBuilder<RelayConfiguration> implements RelayConfiguration {

    private final PathAddress address;
    private final List<ValueDependency<RemoteSiteConfiguration>> sites = new LinkedList<>();
    private volatile String siteName = null;

    public RelayConfigurationBuilder(PathAddress address) {
        super(address);
        this.address = address;
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
    public Builder<RelayConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.sites.clear();
        this.siteName = SITE.resolveModelAttribute(context, model).asString();
        if (model.hasDefined(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property remoteSiteProperty: model.get(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                String remoteSiteName = remoteSiteProperty.getName();
                this.sites.add(new InjectedValueDependency<>(new RemoteSiteConfigurationBuilder(this.address, remoteSiteName), RemoteSiteConfiguration.class));
            }
        }
        return super.configure(context, model);
    }

    @Override
    public RelayConfiguration getValue() {
        return this;
    }

    @Override
    public String getSiteName() {
        return this.siteName;
    }

    @Override
    public List<RemoteSiteConfiguration> getRemoteSites() {
        return this.sites.stream().map(dependency -> dependency.getValue()).collect(Collectors.toList());
    }
}
