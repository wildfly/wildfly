/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
