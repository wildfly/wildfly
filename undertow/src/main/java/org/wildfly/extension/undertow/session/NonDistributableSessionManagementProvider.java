/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.List;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.container.WebDeploymentConfiguration;

import io.undertow.servlet.api.SessionManagerFactory;

/**
 * {@link SessionManagementProvider} for non-distributed web deployments.
 * @author Paul Ferraro
 */
public class NonDistributableSessionManagementProvider implements SessionManagementProvider {
    private final Function<SessionManagerFactoryConfiguration, SessionManagerFactory> factory;

    public NonDistributableSessionManagementProvider(Function<SessionManagerFactoryConfiguration, SessionManagerFactory> factory) {
        this.factory = factory;
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getSessionManagerFactoryServiceConfigurators(ServiceName name, SessionManagerFactoryConfiguration configuration) {
        return List.of(new SessionManagerFactoryServiceConfigurator(name, () -> this.factory.apply(configuration)));
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getSessionAffinityServiceConfigurators(ServiceName name, WebDeploymentConfiguration configuration) {
        CapabilityServiceConfigurator codecConfigurator = new SimpleSessionIdentifierCodecServiceConfigurator(name.append("codec"), configuration.getServerName());
        CapabilityServiceConfigurator locatorConfigurator = new SimpleAffinityLocatorServiceConfigurator(name.append("locator"), configuration.getServerName());
        CapabilityServiceConfigurator wrapperFactoryConfigurator = new SessionConfigWrapperFactoryServiceConfigurator(name, new ServiceSupplierDependency<>(codecConfigurator), new ServiceSupplierDependency<>(locatorConfigurator));
        return List.of(codecConfigurator, locatorConfigurator, wrapperFactoryConfigurator);
    }
}
