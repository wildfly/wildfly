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

package org.wildfly.clustering.web.undertow.routing;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.service.ChildTargetService;
import org.wildfly.clustering.service.FunctionSupplierDependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.service.WebRequirement;
import org.wildfly.clustering.web.service.routing.LegacyRoutingProviderFactory;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.clustering.web.undertow.UndertowUnaryRequirement;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.session.DistributableServerRuntimeHandler;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
@MetaInfServices(DistributableServerRuntimeHandler.class)
public class UndertowDistributableServerRuntimeHandler implements DistributableServerRuntimeHandler {

    private final LegacyRoutingProviderFactory legacyProviderFactory;

    public UndertowDistributableServerRuntimeHandler() {
        Iterator<LegacyRoutingProviderFactory> factories = ServiceLoader.load(LegacyRoutingProviderFactory.class, LegacyRoutingProviderFactory.class.getClassLoader()).iterator();
        if (!factories.hasNext()) {
            throw new ServiceConfigurationError(LegacyRoutingProviderFactory.class.getName());
        }
        this.legacyProviderFactory = factories.next();
    }

    @Override
    public void execute(OperationContext context, String serverName) {
        SupplierDependency<RoutingProvider> provider = this.getRoutingProvider(context, serverName);
        if (provider != null) {
            ServiceTarget target = context.getServiceTarget();
            CapabilityServiceSupport support = context.getCapabilityServiceSupport();
            SupplierDependency<Server> server = new ServiceSupplierDependency<>(UndertowUnaryRequirement.SERVER.getServiceName(context, serverName));
            SupplierDependency<String> route = new FunctionSupplierDependency<>(server, Server::getRoute);
            Consumer<ServiceTarget> installer = new Consumer<>() {
                @Override
                public void accept(ServiceTarget target) {
                    for (CapabilityServiceConfigurator configurator : provider.get().getServiceConfigurators(serverName, route)) {
                        configurator.configure(support).build(target).install();
                    }
                }
            };
            ServiceName name = ServiceName.JBOSS.append("clustering", "web", "undertow", "routing", serverName);
            provider.register(target.addService(name)).setInstance(new ChildTargetService(installer)).install();
        }
    }

    private SupplierDependency<RoutingProvider> getRoutingProvider(OperationContext context, String serverName) {
        if (context.hasOptionalCapability(WebRequirement.ROUTING_PROVIDER.getName(), UndertowUnaryRequirement.SERVER.resolve(serverName), null)) {
            return new ServiceSupplierDependency<>(WebRequirement.ROUTING_PROVIDER.getServiceName(context));
        }
        UndertowClusteringLogger.ROOT_LOGGER.legacyRoutingProviderInUse(serverName);
        return new SimpleSupplierDependency<>(this.legacyProviderFactory.createRoutingProvider());
    }
}
