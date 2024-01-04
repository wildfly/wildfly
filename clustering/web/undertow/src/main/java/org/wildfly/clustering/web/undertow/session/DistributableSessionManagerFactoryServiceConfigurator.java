/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.servlet.ServletContext;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;

import io.undertow.servlet.api.SessionManagerFactory;

/**
 * Distributable {@link SessionManagerFactory} builder for Undertow.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactoryServiceConfigurator<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<org.wildfly.clustering.web.session.SessionManagerFactory<ServletContext, Map<String, Object>, Batch>, SessionManagerFactory> {

    private final SessionManagerFactoryConfiguration configuration;
    private final SupplierDependency<org.wildfly.clustering.web.session.SessionManagerFactory<ServletContext, Map<String, Object>, Batch>> dependency;

    public DistributableSessionManagerFactoryServiceConfigurator(ServiceName name, SessionManagerFactoryConfiguration configuration, SupplierDependency<org.wildfly.clustering.web.session.SessionManagerFactory<ServletContext, Map<String, Object>, Batch>> dependency) {
        super(name);
        this.configuration = configuration;
        this.dependency = dependency;
    }

    @Override
    public SessionManagerFactory apply(org.wildfly.clustering.web.session.SessionManagerFactory<ServletContext, Map<String, Object>, Batch> factory) {
        return new DistributableSessionManagerFactory(factory, this.configuration);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SessionManagerFactory> factory = this.dependency.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(factory, this, this.dependency);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
