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
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

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

    private static final Optional<RouteLocatorBuilderProvider> PROVIDER = StreamSupport.stream(ServiceLoader.load(RouteLocatorBuilderProvider.class, RouteLocatorBuilderProvider.class.getClassLoader()).spliterator(), false).findFirst();

    @Override
    public CapabilityServiceBuilder<SessionIdentifierCodec> getDeploymentBuilder(ServiceName name, String serverName, String deploymentName) {
        Optional<CapabilityServiceBuilder<SessionIdentifierCodec>> builder = PROVIDER.map(provider -> new DistributableSessionIdentifierCodecBuilder(name, serverName, deploymentName, provider));
        return builder.orElse(new SimpleSessionIdentifierCodecBuilder(name, serverName));
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getServerBuilders(String serverName) {
        Collection<CapabilityServiceBuilder<?>> builders = new LinkedList<>();
        CapabilityServiceBuilder<String> routeBuilder = new RouteBuilder(serverName);
        builders.add(routeBuilder);
        Supplier<ValueDependency<String>> routeDependencyProvider = () -> new InjectedValueDependency<>(routeBuilder, String.class);
        PROVIDER.ifPresent(provider -> builders.addAll(provider.getRouteLocatorConfigurationBuilders(serverName, routeDependencyProvider)));
        return builders;
    }
}
