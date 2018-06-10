/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.session.RouteLocatorServiceConfiguratorProvider;
import org.wildfly.extension.undertow.session.SimpleSessionIdentifierCodecServiceConfigurator;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecServiceConfiguratorProvider.class)
public class DistributableSessionIdentifierCodecServiceConfiguratorProvider implements org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecServiceConfiguratorProvider {

    private static final RouteLocatorServiceConfiguratorProvider PROVIDER = loadProvider();

    private static RouteLocatorServiceConfiguratorProvider loadProvider() {
        for (RouteLocatorServiceConfiguratorProvider provider : ServiceLoader.load(RouteLocatorServiceConfiguratorProvider.class, RouteLocatorServiceConfiguratorProvider.class.getClassLoader())) {
            return provider;
        }
        return null;
    }

    @Override
    public CapabilityServiceConfigurator getDeploymentServiceConfigurator(ServiceName name, String serverName, String deploymentName) {
        return (PROVIDER != null) ? new DistributableSessionIdentifierCodecServiceConfigurator(name, serverName, deploymentName, PROVIDER) : new SimpleSessionIdentifierCodecServiceConfigurator(name, serverName);
    }

    @Override
    public Collection<CapabilityServiceConfigurator> getServerServiceConfigurators(String serverName) {
        Collection<CapabilityServiceConfigurator> configurators = new LinkedList<>();
        CapabilityServiceConfigurator routeConfigurator = new RouteServiceConfigurator(serverName);
        configurators.add(routeConfigurator);
        SupplierDependency<String> routeDependency = new ServiceSupplierDependency<>(routeConfigurator.getServiceName());
        if (PROVIDER != null) {
            configurators.addAll(PROVIDER.getRouteLocatorConfigurationServiceConfigurators(serverName, routeDependency));
        }
        return configurators;
    }
}
