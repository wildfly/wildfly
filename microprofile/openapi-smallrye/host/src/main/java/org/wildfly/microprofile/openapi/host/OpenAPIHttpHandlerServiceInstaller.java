/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.openapi.api.OpenApiConfig;
import io.undertow.server.HttpHandler;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceController;
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
import org.wildfly.extension.undertow.Host;
import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service that registers HttpHandler for the OpenAPI endpoint of a host.
 * @author Paul Ferraro
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
        OpenApiConfig configuration = this.configuration.getOpenApiConfig();
        ServiceDependency<Host> host = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName);
        ServiceDependency<OpenAPIModelProvider> provider = ServiceDependency.on(OpenAPIModelProvider.SERVICE_DESCRIPTOR, serverName, hostName, modelName);

        Supplier<HttpHandler> factory = new Supplier<>() {
            @Override
            public HttpHandler get() {
                return new OpenAPIHttpHandler(() -> provider.get().getModel(), configuration);
            }
        };
        Consumer<HttpHandler> start = new Consumer<>() {
            @Override
            public void accept(HttpHandler handler) {
                host.get().registerHandler(path, handler);
            }
        };
        Consumer<HttpHandler> stop = new Consumer<>() {
            @Override
            public void accept(HttpHandler handler) {
                host.get().unregisterHandler(path);
            }
        };
        return ServiceInstaller.builder(factory)
                .startWhen(StartWhen.AVAILABLE)
                .requires(List.of(host, provider))
                .onStart(start)
                .onStop(stop)
                .build()
                .install(target);
    }
}
