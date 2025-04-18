/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.host;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.smallrye.openapi.spi.OASFactoryResolverImpl;

import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.controller.OperationContext;
import org.kohsuke.MetaInfServices;
import org.wildfly.undertow.service.HostServiceInstallerProvider;
import org.wildfly.extension.microprofile.openapi.MicroProfileOpenAPISubsystemRegistrar;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Provides an installer for services providing an OpenAPI endpoint handler for a given host.
 * @author Paul Ferraro
 */
@MetaInfServices(HostServiceInstallerProvider.class)
public class OpenAPIHostServiceInstallerProvider implements HostServiceInstallerProvider {

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
                if (context.getCapabilityServiceSupport().hasCapability(MicroProfileOpenAPISubsystemRegistrar.SERVICE_DESCRIPTOR)) {
                    OpenAPIModelConfiguration configuration = new HostOpenAPIModelConfiguration(serverName, hostName);
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
