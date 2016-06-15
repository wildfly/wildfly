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

package org.wildfly.clustering.server.group;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * Builds a non-clustered cache-based {@link Group} service.
 * @author Paul Ferraro
 */
public class LocalCacheGroupBuilder implements CapabilityServiceBuilder<Group> {

    private final ServiceName name;
    private final String containerName;

    private volatile ValueDependency<JGroupsNodeFactory> factory;

    public LocalCacheGroupBuilder(ServiceName name, String containerName) {
        this.name = name;
        this.containerName = containerName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<Group> configure(CapabilityServiceSupport support) {
        this.factory = new InjectedValueDependency<>(ClusteringRequirement.NODE_FACTORY.getServiceName(support, this.containerName), JGroupsNodeFactory.class);
        return this;
    }

    @Override
    public ServiceBuilder<Group> build(ServiceTarget target) {
        Value<Group> value = () -> new LocalGroup(this.containerName, this.factory.getValue().createNode(null));
        return this.factory.register(target.addService(this.name, new ValueService<>(value)).setInitialMode(ServiceController.Mode.ON_DEMAND));
    }
}
