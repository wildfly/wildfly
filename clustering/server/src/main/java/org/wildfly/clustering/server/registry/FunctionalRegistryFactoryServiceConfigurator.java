/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.registry;

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
