/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.web.session.AffinityLocator;
import org.jboss.as.web.session.SimpleAffinityLocator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Server;

/**
 * Configures a {@link Service} providing a non-distributable {@link AffinityLocator} implementation.
 *
 * @author Radoslav Husar
 */
class SimpleAffinityLocatorServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<Server, AffinityLocator> {

    private final String serverName;

    private volatile SupplierDependency<Server> server;

    public SimpleAffinityLocatorServiceConfigurator(ServiceName name, String serverName) {
        super(name);
        this.serverName = serverName;
    }

    @Override
    public AffinityLocator apply(Server server) {
        return new SimpleAffinityLocator(server.getRoute());
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.server = new ServiceSupplierDependency<>(support.getCapabilityServiceName(Capabilities.CAPABILITY_SERVER, this.serverName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<AffinityLocator> locator = this.server.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(locator, this, this.server);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
