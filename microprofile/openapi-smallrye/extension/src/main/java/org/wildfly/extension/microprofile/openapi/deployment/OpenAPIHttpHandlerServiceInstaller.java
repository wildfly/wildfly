/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import static org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger.LOGGER;

import java.util.List;

import io.smallrye.openapi.api.SmallRyeOpenAPI;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.wildfly.extension.undertow.Host;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Service that registers the OpenAPI HttpHandler.
 * @author Paul Ferraro
 * @author Michael Edgar
 */
public class OpenAPIHttpHandlerServiceInstaller implements DeploymentServiceInstaller {

    private final OpenAPIEndpointConfiguration configuration;

    public OpenAPIHttpHandlerServiceInstaller(OpenAPIEndpointConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void install(DeploymentPhaseContext context) {
        String serverName = this.configuration.getServerName();
        String hostName = this.configuration.getHostName();
        String path = this.configuration.getPath();
        ServiceDependency<Host> host = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName);
        ServiceDependency<SmallRyeOpenAPI> model = ServiceDependency.on(OpenAPIModelConfiguration.SERVICE_DESCRIPTOR, serverName, hostName, path);

        Runnable start = new Runnable() {
            @Override
            public void run() {
                host.get().registerHandler(path, new OpenAPIHttpHandler(model.get()));
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
        ServiceInstaller.builder(start, stop)
            .requires(List.of(host, model))
            .startWhen(StartWhen.INSTALLED)
            .build()
            .install(context);
    }
}
