/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
