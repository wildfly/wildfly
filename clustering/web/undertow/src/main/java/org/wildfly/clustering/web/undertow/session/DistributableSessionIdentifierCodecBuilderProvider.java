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

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.web.session.RouteLocatorBuilderProvider;
import org.wildfly.extension.undertow.session.SimpleSessionIdentifierCodecBuilder;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecBuilderProvider.class)
public class DistributableSessionIdentifierCodecBuilderProvider implements org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecBuilderProvider {

    private static final RouteLocatorBuilderProvider PROVIDER = loadProvider();

    private static RouteLocatorBuilderProvider loadProvider() {
        for (RouteLocatorBuilderProvider provider : ServiceLoader.load(RouteLocatorBuilderProvider.class, RouteLocatorBuilderProvider.class.getClassLoader())) {
            return provider;
        }
        return null;
    }

    @Override
    public CapabilityServiceBuilder<SessionIdentifierCodec> getDeploymentBuilder(ServiceName name, String serverName, String deploymentName) {
        return (PROVIDER != null) ? new DistributableSessionIdentifierCodecBuilder(name, serverName, deploymentName, PROVIDER) : new SimpleSessionIdentifierCodecBuilder(name, serverName);
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getServerBuilders(String serverName) {
        Collection<CapabilityServiceBuilder<?>> builders = new LinkedList<>();
        CapabilityServiceBuilder<String> routeBuilder = new RouteBuilder(serverName);
        builders.add(routeBuilder);
        ValueDependency<String> routeDependency = new InjectedValueDependency<>(routeBuilder, String.class);
        if (PROVIDER != null) {
            builders.addAll(PROVIDER.getRouteLocatorConfigurationBuilders(serverName, routeDependency));
        }
        return builders;
    }
}
