/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.infinispan.group.LocalGroup;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Builds a non-clustered {@link Group}.
 * @author Paul Ferraro
 */
public class LocalGroupServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<ServerEnvironment, Group> {

    private final SupplierDependency<ServerEnvironment> environment;

    public LocalGroupServiceConfigurator(ServiceName name) {
        super(name);
        this.environment = new ServiceSupplierDependency<>(ServerEnvironmentService.SERVICE_NAME);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<Group> group = this.environment.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(group, this, this.environment);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Group apply(ServerEnvironment environment) {
        return new LocalGroup(environment.getNodeName(), org.wildfly.clustering.server.service.LocalGroupServiceConfiguratorProvider.LOCAL);
    }
}
