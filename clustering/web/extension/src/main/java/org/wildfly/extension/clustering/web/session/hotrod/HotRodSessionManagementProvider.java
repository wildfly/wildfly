/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.session.hotrod;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.extension.clustering.web.routing.LocalRouteLocatorServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionManagementProvider implements DistributableSessionManagementProvider<HotRodSessionManagementConfiguration<DeploymentUnit>> {

    private final HotRodSessionManagementConfiguration<DeploymentUnit> configuration;

    public HotRodSessionManagementProvider(HotRodSessionManagementConfiguration<DeploymentUnit> configuration) {
        this.configuration = configuration;
    }

    @Override
    public <S, SC, AL, LC> CapabilityServiceConfigurator getSessionManagerFactoryServiceConfigurator(SessionManagerFactoryConfiguration<S, SC, AL, LC> config) {
        return new HotRodSessionManagerFactoryServiceConfigurator<>(this.configuration, config);
    }

    @Override
    public CapabilityServiceConfigurator getRouteLocatorServiceConfigurator(WebDeploymentConfiguration configuration) {
        return new LocalRouteLocatorServiceConfigurator(configuration);
    }

    @Override
    public HotRodSessionManagementConfiguration<DeploymentUnit> getSessionManagementConfiguration() {
        return this.configuration;
    }
}
