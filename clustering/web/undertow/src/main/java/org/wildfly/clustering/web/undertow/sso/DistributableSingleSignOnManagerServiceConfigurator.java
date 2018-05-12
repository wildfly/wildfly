/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.sso;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryServiceConfiguratorProvider;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.impl.SingleSignOnManager;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.server.session.SessionListener;


/**
 * Builds a distributable {@link SingleSignOnManagerFactory} service.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<SingleSignOnManager> {

    private final SupplierDependency<SSOManager<AuthenticatedSession, String, String, Void, Batch>> manager;
    private final SupplierDependency<SessionManagerRegistry> registry;

    private final Collection<CapabilityServiceConfigurator> configurators;

    public DistributableSingleSignOnManagerServiceConfigurator(ServiceName name, String serverName, String hostName, SSOManagerFactoryServiceConfiguratorProvider provider) {
        super(name);

        CapabilityServiceConfigurator factoryConfigurator = provider.getServiceConfigurator(hostName);
        ServiceName generatorServiceName = name.append("generator");
        CapabilityServiceConfigurator generatorConfigurator = new SessionIdGeneratorServiceConfigurator(generatorServiceName, serverName);

        SupplierDependency<SSOManagerFactory<AuthenticatedSession, String, String, Batch>> factoryDependency = new ServiceSupplierDependency<>(factoryConfigurator);
        SupplierDependency<SessionIdGenerator> generatorDependency = new ServiceSupplierDependency<>(generatorServiceName);
        ServiceName managerServiceName = name.append("manager");
        CapabilityServiceConfigurator managerConfigurator = new SSOManagerServiceConfigurator<>(managerServiceName, factoryDependency, generatorDependency, () -> null);

        SupplierDependency<SSOManager<AuthenticatedSession, String, String, Void, Batch>> managerDependency = new ServiceSupplierDependency<>(managerServiceName);
        ServiceName listenerServiceName = name.append("listener");
        CapabilityServiceConfigurator listenerConfigurator = new SessionListenerServiceConfigurator(listenerServiceName, managerDependency);

        SupplierDependency<SessionListener> listenerDependency = new ServiceSupplierDependency<>(listenerServiceName);
        ServiceName registryServiceName = name.append("registry");
        CapabilityServiceConfigurator registryConfigurator = new SessionManagerRegistryServiceConfigurator(registryServiceName, serverName, hostName, listenerDependency);

        this.manager = new ServiceSupplierDependency<>(managerConfigurator);
        this.registry = new ServiceSupplierDependency<>(registryConfigurator);

        this.configurators = Arrays.asList(factoryConfigurator, generatorConfigurator, managerConfigurator, listenerConfigurator, registryConfigurator);
    }

    @Override
    public SingleSignOnManager get() {
        return new DistributableSingleSignOnManager(this.manager.get(), this.registry.get());
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        for (CapabilityServiceConfigurator configurator : this.configurators) {
            configurator.configure(support);
        }
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        for (CapabilityServiceConfigurator configurator : this.configurators) {
            configurator.build(target).install();
        }
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SingleSignOnManager> manager = new CompositeDependency(this.manager, this.registry).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(manager, Function.identity(), this);
        return builder.setInstance(service);
    }
}
