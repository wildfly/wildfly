/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.host;

import static org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger.LOGGER;

import java.util.List;

import io.smallrye.openapi.api.OpenApiConfig;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.microprofile.openapi.model.OpenAPIProvider;
import org.wildfly.extension.undertow.Host;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service that registers HttpHandler for the OpenAPI endpoint of a host.
 * @author Paul Ferraro
 * @author Michael Edgar
 */
public class OpenAPIHttpHandlerServiceInstaller implements ServiceInstaller {

    private final OpenAPIModelConfiguration configuration;

    public OpenAPIHttpHandlerServiceInstaller(OpenAPIModelConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        String serverName = this.configuration.getServerName();
        String hostName = this.configuration.getHostName();
        String modelName = this.configuration.getModelName();
        String path = this.configuration.getPath();
        OpenApiConfig configuration = this.configuration.getConfiguration();
        ServiceDependency<Host> host = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName);
        ServiceDependency<OpenAPIProvider> provider = ServiceDependency.on(OpenAPIProvider.SERVICE_DESCRIPTOR, serverName, hostName, modelName);

        Runnable start = new Runnable() {
            @Override
            public void run() {
                host.get().registerHandler(path, new OpenAPIHttpHandler(provider.get(), configuration));
                LOGGER.endpointRegistered(path, host.get().getName());
            }
        };
        Runnable stop = new Runnable() {
            @Override
            public void run() {
                host.get().unregisterHandler(path);
                LOGGER.endpointUnregistered(path, host.get().getName());
            }
        };
        return ServiceInstaller.builder(start, stop).asPassive()
            .requires(List.of(host, provider))
            .build()
            .install(target);
    }
}
