/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.List;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.server.service.GroupCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.service.GroupServiceConfiguratorProvider;
import org.wildfly.clustering.service.ServiceConfigurator;

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
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String group) {
        ServiceName name = this.requirement.getServiceName(support, group);
        ServiceConfigurator configurator = this.factory.createServiceConfigurator(name, group).configure(support);
        if (this.jndiNameFactory == null) {
            return List.of(configurator);
        }
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(group).getAbsoluteName());
        ServiceConfigurator binderConfigurator = new BinderServiceConfigurator(binding, configurator.getServiceName()).configure(support);
        return List.of(configurator, binderConfigurator);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
