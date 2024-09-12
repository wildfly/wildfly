/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.session;

import java.util.function.Supplier;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.server.deployment.DeploymentConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractSessionManagementProvider implements DistributableSessionManagementProvider {

    private final DistributableSessionManagementConfiguration<DeploymentUnit> configuration;
    private final Supplier<RouteLocatorProvider> locatorProviderFactory;
    private final BinaryServiceConfiguration cacheConfiguration;

    protected AbstractSessionManagementProvider(DistributableSessionManagementConfiguration<DeploymentUnit> configuration, BinaryServiceConfiguration cacheConfiguration, Supplier<RouteLocatorProvider> locatorProviderFactory) {
        this.configuration = configuration;
        this.locatorProviderFactory = locatorProviderFactory;
        this.cacheConfiguration = cacheConfiguration;
    }

    @Override
    public DeploymentServiceInstaller getRouteLocatorServiceInstaller(DeploymentPhaseContext context, DeploymentConfiguration configuration) {
        return this.getRouteLocatorProvider().getServiceInstaller(context, this.cacheConfiguration, configuration);
    }

    @Override
    public DistributableSessionManagementConfiguration<DeploymentUnit> getSessionManagementConfiguration() {
        return this.configuration;
    }

    public BinaryServiceConfiguration getCacheConfiguration() {
        return this.cacheConfiguration;
    }

    public RouteLocatorProvider getRouteLocatorProvider() {
        return this.locatorProviderFactory.get();
    }
}
