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

package org.wildfly.clustering.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Configures a {@link Service} whose value is provided by another {@link Service}.
 * @author Paul Ferraro
 */
public class IdentityServiceConfigurator<T> extends SimpleServiceNameProvider implements ServiceConfigurator {

    private final ServiceName requirementName;
    private final ServiceController.Mode initialMode;

    /**
     * Constructs a new service configurator.
     * @param name the target service name
     * @param targetName the target service
     */
    public IdentityServiceConfigurator(ServiceName name, ServiceName requirementName) {
        this(name, requirementName, ServiceController.Mode.PASSIVE);
    }

    /**
     * Constructs a new service configurator.
     * @param name the target service name
     * @param targetName the target service
     * @param initialMode the initial mode of the configured service.
     */
    public IdentityServiceConfigurator(ServiceName name, ServiceName requirementName, ServiceController.Mode initialMode) {
        super(name);
        assert initialMode != ServiceController.Mode.REMOVE;
        this.requirementName = requirementName;
        this.initialMode = initialMode;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<T> injector = builder.provides(this.getServiceName());
        Supplier<T> requirement = builder.requires(this.requirementName);
        Service service = new FunctionalService<>(injector, Function.identity(), requirement);
        return builder.setInstance(service).setInitialMode(this.initialMode);
    }
}
