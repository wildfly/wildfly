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

package org.wildfly.extension.undertow.session;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

import io.undertow.servlet.api.SessionManagerFactory;

/**
 * Configures a service providing a {@link SessionManagerFactory}.
 * @author Paul Ferraro
 */
public class SessionManagerFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator {

    private final Supplier<SessionManagerFactory> provider;

    public SessionManagerFactoryServiceConfigurator(ServiceName name, Supplier<SessionManagerFactory> provider) {
        super(name);
        this.provider = provider;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SessionManagerFactory> injector = builder.provides(this.getServiceName());
        return builder.setInstance(Service.newInstance(injector, this.provider.get()));
    }
}
