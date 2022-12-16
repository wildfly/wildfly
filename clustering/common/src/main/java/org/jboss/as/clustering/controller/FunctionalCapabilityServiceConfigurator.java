/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Configures a service that provides a value created from a generic factory and mapper.
 * @author Paul Ferraro
 * @param <T> the source type of the mapped value provided by the installed service
 * @param <V> the type of the value provided by the installed service
 */
public class FunctionalCapabilityServiceConfigurator<T, V> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator {

    private final Function<T, V> mapper;
    private final Supplier<T> factory;

    public FunctionalCapabilityServiceConfigurator(ServiceName name, Function<T, V> mapper, Supplier<T> factory) {
        super(name);
        this.mapper = mapper;
        this.factory = factory;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<V> injector = builder.provides(name);
        return builder.setInstance(new FunctionalService<>(injector, this.mapper, this.factory));
    }
}
