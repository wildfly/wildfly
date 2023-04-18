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
