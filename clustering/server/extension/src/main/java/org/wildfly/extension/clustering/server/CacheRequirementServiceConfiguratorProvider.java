/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.List;
import java.util.function.BiFunction;

import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.service.CacheCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.service.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class CacheRequirementServiceConfiguratorProvider<T> implements CacheServiceConfiguratorProvider {

    private final ClusteringCacheRequirement requirement;
    private final CacheCapabilityServiceConfiguratorFactory<T> factory;
    private final BiFunction<String, String, JndiName> jndiNameFactory;

    protected CacheRequirementServiceConfiguratorProvider(ClusteringCacheRequirement requirement, CacheCapabilityServiceConfiguratorFactory<T> factory) {
        this(requirement, factory, null);
    }

    protected CacheRequirementServiceConfiguratorProvider(ClusteringCacheRequirement requirement, CacheCapabilityServiceConfiguratorFactory<T> factory, BiFunction<String, String, JndiName> jndiNameFactory) {
        this.requirement = requirement;
        this.factory = factory;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String containerName, String cacheName) {
        ServiceName name = this.requirement.getServiceName(support, containerName, cacheName);
        ServiceConfigurator configurator = this.factory.createServiceConfigurator(name, containerName, cacheName).configure(support);
        if (this.jndiNameFactory == null) {
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
