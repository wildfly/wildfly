/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
