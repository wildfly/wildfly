/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.controller.PathAddress;
import org.jboss.modcluster.ModClusterService;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Service starting mod_cluster service.
 *
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
public class ContainerEventHandlerServiceConfigurator extends CapabilityServiceNameProvider implements ServiceConfigurator, Supplier<ModClusterService>, Consumer<ModClusterService> {

    private final String proxyName;
    private final LoadBalanceFactorProvider factorProvider;
    private final SupplierDependency<ModClusterConfiguration> configuration;


    ContainerEventHandlerServiceConfigurator(PathAddress address, LoadBalanceFactorProvider factorProvider) {
        super(ProxyConfigurationResourceDefinition.Capability.SERVICE, address);
        this.configuration = new ServiceSupplierDependency<>(new ProxyConfigurationServiceConfigurator(address));
        this.proxyName = address.getLastElement().getValue();
        this.factorProvider = factorProvider;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        Consumer<ModClusterService> modClusterService = this.configuration.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(modClusterService, Function.identity(), this, this);
        return builder.setInstance(service);
    }

    @Override
    public ModClusterService get() {
        ROOT_LOGGER.debugf("Starting mod_cluster service for proxy '%s'.", proxyName);

        return new ModClusterService(this.configuration.get(), this.factorProvider);
    }

    @Override
    public void accept(ModClusterService service) {
        ROOT_LOGGER.debugf("Stopping mod_cluster service for proxy '%s'.", proxyName);

        service.shutdown();
    }
}
