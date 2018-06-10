/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.wildfly.extension.mod_cluster;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.modcluster.ModClusterService;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Service starting mod_cluster service.
 *
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
public class ContainerEventHandlerServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, Supplier<ModClusterService>, Consumer<ModClusterService> {

    public static final ServiceName SERVICE_NAME = ModClusterConfigResourceDefinition.MOD_CLUSTER_CAPABILITY.getCapabilityServiceName();
    public static final ServiceName CONFIG_SERVICE_NAME = SERVICE_NAME.append("config");

    private final LoadBalanceFactorProvider load;

    private volatile Supplier<ModClusterConfiguration> configuration;

    ContainerEventHandlerServiceConfigurator(LoadBalanceFactorProvider factorProvider) {
        super(SERVICE_NAME);
        this.load = factorProvider;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        // Temporary alias, in case anyone is using old service name
        Consumer<ModClusterService> modClusterService = builder.addAliases(ServiceName.JBOSS.append(ModClusterExtension.SUBSYSTEM_NAME)).provides(this.getServiceName());
        this.configuration = builder.requires(ContainerEventHandlerServiceConfigurator.CONFIG_SERVICE_NAME);
        Service service = new FunctionalService<>(modClusterService, Function.identity(), this, this);
        return builder.setInstance(service);
    }

    @Override
    public ModClusterService get() {
        return new ModClusterService(this.configuration.get(), this.load);
    }

    @Override
    public void accept(ModClusterService service) {
        service.shutdown();
    }
}
