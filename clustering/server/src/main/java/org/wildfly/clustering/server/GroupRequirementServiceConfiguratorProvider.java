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

package org.wildfly.clustering.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.GroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class GroupRequirementServiceConfiguratorProvider<T> implements GroupServiceConfiguratorProvider {

    private final ClusteringRequirement requirement;
    private final GroupCapabilityServiceConfiguratorFactory<T> factory;
    private final Function<String, JndiName> jndiNameFactory;

    protected GroupRequirementServiceConfiguratorProvider(ClusteringRequirement requirement, GroupCapabilityServiceConfiguratorFactory<T> factory) {
        this(requirement, factory, null);
    }

    protected GroupRequirementServiceConfiguratorProvider(ClusteringRequirement requirement, GroupCapabilityServiceConfiguratorFactory<T> factory, Function<String, JndiName> jndiNameFactory) {
        this.requirement = requirement;
        this.factory = factory;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Collection<CapabilityServiceConfigurator> getServiceConfigurators(ServiceNameRegistry<ClusteringRequirement> registry, String group) {
        CapabilityServiceConfigurator configurator = this.factory.createServiceConfigurator(registry.getServiceName(this.requirement), group);
        if (this.jndiNameFactory == null) {
            return Collections.singleton(configurator);
        }
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(group).getAbsoluteName());
        CapabilityServiceConfigurator binderConfigurator = new BinderServiceConfigurator(binding, configurator.getServiceName());
        return Arrays.asList(configurator, binderConfigurator);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
