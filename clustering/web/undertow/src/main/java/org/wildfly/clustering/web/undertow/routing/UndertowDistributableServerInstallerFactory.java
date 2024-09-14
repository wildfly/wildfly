/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.routing;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.ServiceController;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.service.routing.LegacyRoutingProviderFactory;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.session.DistributableServerServiceInstallerFactory;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
@MetaInfServices(DistributableServerServiceInstallerFactory.class)
public class UndertowDistributableServerInstallerFactory implements DistributableServerServiceInstallerFactory {

    private final LegacyRoutingProviderFactory legacyProviderFactory;

    public UndertowDistributableServerInstallerFactory() {
        Iterator<LegacyRoutingProviderFactory> factories = ServiceLoader.load(LegacyRoutingProviderFactory.class, LegacyRoutingProviderFactory.class.getClassLoader()).iterator();
        if (!factories.hasNext()) {
            throw new ServiceConfigurationError(LegacyRoutingProviderFactory.class.getName());
        }
        this.legacyProviderFactory = factories.next();
    }

    @Override
    public ResourceServiceInstaller getServiceInstaller(OperationContext context, String serverName) {
        ServiceDependency<RoutingProvider> provider = this.getRoutingProvider(context, serverName);
        ServiceInstaller installer = new ServiceInstaller() {
            @Override
            public ServiceController<?> install(RequirementServiceTarget target) {
                ServiceDependency<String> route = ServiceDependency.on(Server.SERVICE_DESCRIPTOR, serverName).map(Server::getRoute);
                for (ServiceInstaller installer : provider.get().getServiceInstallers(serverName, route)) {
                    installer.install(target);
                }
                return null;
            }
        };
        return ServiceInstaller.builder(installer, context.getCapabilityServiceSupport()).requires(provider).build();
    }

    private ServiceDependency<RoutingProvider> getRoutingProvider(OperationContext context, String serverName) {
        if (context.hasOptionalCapability(RoutingProvider.SERVICE_DESCRIPTOR.getName(), RuntimeCapability.resolveCapabilityName(Server.SERVICE_DESCRIPTOR, serverName), null)) {
            return ServiceDependency.on(RoutingProvider.SERVICE_DESCRIPTOR);
        }
        UndertowClusteringLogger.ROOT_LOGGER.legacyRoutingProviderInUse(serverName);
        return ServiceDependency.of(this.legacyProviderFactory.createRoutingProvider());
    }
}
