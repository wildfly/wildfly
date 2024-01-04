/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.session.infinispan;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;

/**
 * An Infinispan cache-based {@link DistributableSessionManagementProvider}.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementProvider implements DistributableSessionManagementProvider<InfinispanSessionManagementConfiguration<DeploymentUnit>> {

    private final InfinispanSessionManagementConfiguration<DeploymentUnit> configuration;
    private final RouteLocatorServiceConfiguratorFactory<InfinispanSessionManagementConfiguration<DeploymentUnit>> factory;

    public InfinispanSessionManagementProvider(InfinispanSessionManagementConfiguration<DeploymentUnit> configuration, RouteLocatorServiceConfiguratorFactory<InfinispanSessionManagementConfiguration<DeploymentUnit>> factory) {
        this.configuration = configuration;
        this.factory = factory;
    }

    @Override
    public <S, SC, AL, LC> CapabilityServiceConfigurator getSessionManagerFactoryServiceConfigurator(SessionManagerFactoryConfiguration<S, SC, AL, LC> config) {
        return new InfinispanSessionManagerFactoryServiceConfigurator<>(this.configuration, config);
    }

    @Override
    public CapabilityServiceConfigurator getRouteLocatorServiceConfigurator(WebDeploymentConfiguration config) {
        return this.factory.createRouteLocatorServiceConfigurator(this.configuration, config);
    }

    @Override
    public InfinispanSessionManagementConfiguration<DeploymentUnit> getSessionManagementConfiguration() {
        return this.configuration;
    }

    public RouteLocatorServiceConfiguratorFactory<InfinispanSessionManagementConfiguration<DeploymentUnit>> getRouteLocatorServiceConfiguratorFactory() {
        return this.factory;
    }
}
