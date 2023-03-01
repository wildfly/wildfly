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

package org.wildfly.clustering.web.undertow.session;

import java.util.List;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.container.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.undertow.routing.DistributableAffinityLocatorServiceConfigurator;
import org.wildfly.clustering.web.undertow.routing.DistributableSessionIdentifierCodecServiceConfigurator;
import org.wildfly.extension.undertow.session.SessionConfigWrapperFactoryServiceConfigurator;

/**
 * {@link SessionManagementProvider} for Undertow.
 * @author Paul Ferraro
 */
public class UndertowDistributableSessionManagementProvider<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> implements SessionManagementProvider {

    private final DistributableSessionManagementProvider<C> provider;
    private final Immutability immutability;

    public UndertowDistributableSessionManagementProvider(DistributableSessionManagementProvider<C> provider, Immutability immutability) {
        this.provider = provider;
        this.immutability = immutability;
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getSessionManagerFactoryServiceConfigurators(ServiceName name, SessionManagerFactoryConfiguration configuration) {
        CapabilityServiceConfigurator configurator = this.provider.getSessionManagerFactoryServiceConfigurator(new SessionManagerFactoryConfigurationAdapter<>(configuration, this.provider.getSessionManagementConfiguration(), this.immutability));
        return List.of(configurator, new DistributableSessionManagerFactoryServiceConfigurator<>(name, configuration, new ServiceSupplierDependency<>(configurator)));
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getSessionAffinityServiceConfigurators(ServiceName name, WebDeploymentConfiguration configuration) {
        CapabilityServiceConfigurator routeLocatorConfigurator = this.provider.getRouteLocatorServiceConfigurator(new WebDeploymentConfigurationAdapter(configuration));
        CapabilityServiceConfigurator codecConfigurator = new DistributableSessionIdentifierCodecServiceConfigurator(name.append("codec"), new ServiceSupplierDependency<>(routeLocatorConfigurator));
        CapabilityServiceConfigurator affinityLocatorConfigurator = new DistributableAffinityLocatorServiceConfigurator(name.append("affinity"), new ServiceSupplierDependency<>(routeLocatorConfigurator));
        CapabilityServiceConfigurator wrapperFactoryConfigurator = new SessionConfigWrapperFactoryServiceConfigurator(name, new ServiceSupplierDependency<>(codecConfigurator), new ServiceSupplierDependency<>(affinityLocatorConfigurator));
        return List.of(routeLocatorConfigurator, codecConfigurator, affinityLocatorConfigurator, wrapperFactoryConfigurator);
    }
}
