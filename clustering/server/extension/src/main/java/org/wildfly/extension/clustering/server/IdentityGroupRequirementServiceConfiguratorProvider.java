/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.List;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.server.service.IdentityGroupServiceConfiguratorProvider;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class IdentityGroupRequirementServiceConfiguratorProvider implements IdentityGroupServiceConfiguratorProvider {

    private final ClusteringRequirement requirement;
    private final Function<String, JndiName> jndiNameFactory;

    protected IdentityGroupRequirementServiceConfiguratorProvider(ClusteringRequirement requirement) {
        this(requirement, null);
    }

    protected IdentityGroupRequirementServiceConfiguratorProvider(ClusteringRequirement requirement, Function<String, JndiName> jndiNameFactory) {
        this.requirement = requirement;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String group, String targetGroup) {
        ServiceName name = this.requirement.getServiceName(support, group);
        ServiceName targetName = this.requirement.getServiceName(support, targetGroup);
        ServiceConfigurator configurator = new IdentityServiceConfigurator<>(name, targetName);
        if ((this.jndiNameFactory == null) || JndiNameFactory.DEFAULT_LOCAL_NAME.equals(targetGroup)) {
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
