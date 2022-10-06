/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
