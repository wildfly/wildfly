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
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class which provides configure(), build() methods used to allow SimpleResourceServiceHandler to install
 * a CacheFactoryBuilder service.
 *
 * The class installs the service using:
 * - a service name obtained from a capability and a path address, and
 * - a service value obtained from this class, via a Supplier<CacheFactoryBuilder>
 *
 * Subclasses should implement the get() method to return the CacheFactoryBuilder.
 */
public abstract class CacheFactoryResourceServiceConfigurator extends CapabilityServiceNameProvider
        implements ResourceServiceConfigurator, Supplier<CacheFactoryBuilder> {

    public CacheFactoryResourceServiceConfigurator(PathAddress address) {
        super(CacheFactoryResourceDefinition.Capability.CACHE_FACTORY, address);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<CacheFactoryBuilder> cacheFactoryBuilder = builder.provides(name);
        Service service = new FunctionalService<>(cacheFactoryBuilder, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
