/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import static org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger.LOGGER;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.extension.undertow.Host;

/**
 * Service that registers the OpenAPI HttpHandler.
 * @author Paul Ferraro
 * @author Michael Edgar
 */
public class OpenAPIHttpHandlerServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, Service {

    private final SupplierDependency<OpenAPI> model;
    private final SupplierDependency<Host> host;
    private final String path;

    public OpenAPIHttpHandlerServiceConfigurator(OpenAPIServiceNameProvider provider) {
        super(provider.getServiceName().append("handler"));
        this.model = new ServiceSupplierDependency<>(provider.getServiceName());
        this.host = new ServiceSupplierDependency<>(provider.getHostServiceName());
        this.path = provider.getPath();
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        new CompositeDependency(this.model, this.host).register(builder);
        return builder.setInstance(this);
    }

    @Override
    public void start(StartContext context) {
        Host host = this.host.get();
        host.registerHandler(this.path, new OpenAPIHttpHandler(this.model.get()));

        LOGGER.endpointRegistered(this.path, host.getName());
    }

    @Override
    public void stop(StopContext context) {
        Host host = this.host.get();
        host.unregisterHandler(this.path);

        LOGGER.endpointUnregistered(this.path, host.getName());
    }
}
