/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;

import io.undertow.servlet.api.SessionManagerFactory;

/**
 * Distributable {@link SessionManagerFactory} builder for Undertow.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactoryServiceConfigurator<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<org.wildfly.clustering.web.session.SessionManagerFactory<ServletContext, Map<String, Object>, Batch>, SessionManagerFactory> {

    private final SessionManagerFactoryConfiguration configuration;
    private final CapabilityServiceConfigurator configurator;

    public DistributableSessionManagerFactoryServiceConfigurator(ServiceName name, SessionManagerFactoryConfiguration configuration, DistributableSessionManagementProvider<C> provider, Immutability immutability) {
        super(name);
        this.configuration = configuration;
        this.configurator = provider.getSessionManagerFactoryServiceConfigurator(new SessionManagerFactoryConfigurationAdapter<>(configuration, provider.getSessionManagementConfiguration(), immutability));
    }

    @Override
    public SessionManagerFactory apply(org.wildfly.clustering.web.session.SessionManagerFactory<ServletContext, Map<String, Object>, Batch> factory) {
        return new DistributableSessionManagerFactory(factory, this.configuration);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurator.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurator.build(target).install();

        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Supplier<org.wildfly.clustering.web.session.SessionManagerFactory<ServletContext, Map<String, Object>, Batch>> impl = builder.requires(this.configurator.getServiceName());
        Consumer<SessionManagerFactory> factory = builder.provides(this.getServiceName());
        Service service = new FunctionalService<>(factory, this, impl);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
