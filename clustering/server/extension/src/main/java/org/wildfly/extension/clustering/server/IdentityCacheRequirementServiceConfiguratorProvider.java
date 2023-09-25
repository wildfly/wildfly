/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.List;
import java.util.function.BiFunction;

import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class IdentityCacheRequirementServiceConfiguratorProvider implements IdentityCacheServiceConfiguratorProvider {

    private final ClusteringCacheRequirement requirement;
    private final BiFunction<String, String, JndiName> jndiNameFactory;

    protected IdentityCacheRequirementServiceConfiguratorProvider(ClusteringCacheRequirement requirement) {
        this(requirement, null);
    }

    protected IdentityCacheRequirementServiceConfiguratorProvider(ClusteringCacheRequirement requirement, BiFunction<String, String, JndiName> jndiNameFactory) {
        this.requirement = requirement;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String containerName, String cacheName, String targetCacheName) {
        ServiceName name = this.requirement.getServiceName(support, containerName, cacheName);
        ServiceName targetName = this.requirement.getServiceName(support, containerName, targetCacheName);
        ServiceConfigurator configurator = new IdentityServiceConfigurator<>(name, targetName);
        if ((this.jndiNameFactory == null) || JndiNameFactory.DEFAULT_LOCAL_NAME.equals(targetCacheName)) {
            return List.of(configurator);
        }
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(containerName, cacheName).getAbsoluteName());
        ServiceConfigurator binderConfigurator = new BinderServiceConfigurator(binding, configurator.getServiceName()).configure(support);
        return List.of(configurator, binderConfigurator);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
