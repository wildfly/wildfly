/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
