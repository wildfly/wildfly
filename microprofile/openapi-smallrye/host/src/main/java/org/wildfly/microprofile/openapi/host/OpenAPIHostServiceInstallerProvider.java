/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.smallrye.openapi.spi.OASFactoryResolverImpl;

import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.HostServiceInstallerProvider;
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

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
                List<ResourceServiceInstaller> installers = new ArrayList<>(2);
                RuntimeCapability<?> hostCapability = context.getResourceRegistration().getCapabilities().stream().filter(HOST_FILTER).findFirst().orElse(null);
                if ((hostCapability != null) && context.hasOptionalCapability(OpenAPIModelProvider.SUBSYSTEM_SERVICE_DESCRIPTOR, hostCapability, null)) {
                    HostOpenAPIModelConfiguration configuration = new HostOpenAPIModelConfiguration(serverName, hostName);
                    if (configuration.isEnabled()) {
                        installers.add(new HostOpenAPIProviderServiceInstaller(configuration));

                        installers.add(new OpenAPIHttpHandlerServiceInstaller(configuration));
                    }
                }
                return ResourceServiceInstaller.combine(installers).install(context);
            }
        };
    }
}
