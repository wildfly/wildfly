/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.smallrye.common.function.Functions;
import io.smallrye.openapi.spi.OASFactoryResolverImpl;

import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.msc.service.ServiceController;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.HostServiceInstallerProvider;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Provides an installer for services providing an OpenAPI endpoint handler for a given host.
 * @author Paul Ferraro
 */
@MetaInfServices(HostServiceInstallerProvider.class)
public class OpenAPIHostServiceInstallerProvider implements HostServiceInstallerProvider {
    // N.B. MRR.getCapabilities() returns set of raw types
    @SuppressWarnings("rawtypes")
    private static final Predicate<RuntimeCapability> HOST_FILTER = capability -> capability.getName().equals(Host.SERVICE_DESCRIPTOR.getName());

    static {
        // Set the static OASFactoryResolver eagerly avoiding the need perform TCCL service loading later
        OASFactoryResolver.setInstance(new OASFactoryResolverImpl());
    }

    @Override
    public ResourceServiceInstaller getServiceInstaller(String serverName, String hostName) {
        return new ResourceServiceInstaller() {
            @Override
            public Consumer<OperationContext> install(OperationContext context) {
                RuntimeCapability<?> hostCapability = context.getResourceRegistration().getCapabilities().stream().filter(HOST_FILTER).findFirst().orElse(null);
                if ((hostCapability == null) || !context.hasOptionalCapability(OpenAPIModelProvider.SUBSYSTEM_SERVICE_DESCRIPTOR, hostCapability, null)) {
                    return Functions.discardingConsumer();
                }
                // Collect listeners of the server associated with this host, in case we require them
                Set<String> listeners = new TreeSet<>();
                Resource serverResource = context.readResourceFromRoot(context.getCurrentAddress().getParent());
                for (String childType : serverResource.getChildTypes()) {
                    for (String childName : serverResource.getChildrenNames(childType)) {
                        // Determine if this child resource is a listener
                        if (context.hasOptionalCapability(UndertowListener.SERVICE_DESCRIPTOR, serverName, childName, hostCapability, null)) {
                            listeners.add(childName);
                        }
                    }
                }
                return ServiceInstaller.Builder.of(new ServiceInstaller() {
                    @Override
                    public ServiceController<?> install(RequirementServiceTarget target) {
                        HostOpenAPIModelConfiguration configuration = new HostOpenAPIModelConfiguration(serverName, hostName, listeners);
                        if (configuration.isEnabled()) {
                            new HostOpenAPIProviderServiceInstaller(configuration).install(target);
                            new OpenAPIHttpHandlerServiceInstaller(configuration).install(target);
                        }
                        return null;
                    }
                }, context.getCapabilityServiceSupport()).build().install(context);
            }
        };
    }
}
