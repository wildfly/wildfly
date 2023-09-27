/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.infinispan.registry.FunctionalRegistryFactory;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * @author Paul Ferraro
 */
public abstract class FunctionalRegistryFactoryServiceConfigurator<K, V> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, BiFunction<Map.Entry<K, V>, Runnable, Registry<K, V>>, Dependency {

    public FunctionalRegistryFactoryServiceConfigurator(ServiceName name) {
        super(name);
    }

    @Override
    public final ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<RegistryFactory<K, V>> factory = this.register(builder).provides(this.getServiceName());
        Service service = Service.newInstance(factory, new FunctionalRegistryFactory<>(this));
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
