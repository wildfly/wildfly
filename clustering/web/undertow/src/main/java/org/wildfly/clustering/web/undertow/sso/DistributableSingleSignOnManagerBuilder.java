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

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.impl.SingleSignOnManager;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.server.session.SessionListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilderProvider;


/**
 * Builds a distributable {@link SingleSignOnManagerFactory} service.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerBuilder implements CapabilityServiceBuilder<SingleSignOnManager> {

    private final ServiceName name;

    @SuppressWarnings("rawtypes")
    private final ValueDependency<SSOManager> manager;
    private final ValueDependency<SessionManagerRegistry> registry;

    private final Collection<CapabilityServiceBuilder<?>> builders;

    public DistributableSingleSignOnManagerBuilder(ServiceName name, String serverName, String hostName, SSOManagerFactoryBuilderProvider<Batch> provider) {
        this.name = name;

        CapabilityServiceBuilder<SSOManagerFactory<AuthenticatedSession, String, String, Batch>> factoryBuilder = provider.<AuthenticatedSession, String, String>getBuilder(hostName);
        ServiceName generatorServiceName = this.name.append("generator");
        CapabilityServiceBuilder<SessionIdGenerator> generatorBuilder = new SessionIdGeneratorBuilder(generatorServiceName, serverName);

        @SuppressWarnings("rawtypes")
        ValueDependency<SSOManagerFactory> factoryDependency = new InjectedValueDependency<>(factoryBuilder, SSOManagerFactory.class);
        ValueDependency<SessionIdGenerator> generatorDependency = new InjectedValueDependency<>(generatorServiceName, SessionIdGenerator.class);
        ServiceName managerServiceName = this.name.append("manager");
        CapabilityServiceBuilder<SSOManager<AuthenticatedSession, String, String, Void, Batch>> managerBuilder = new SSOManagerBuilder<>(managerServiceName, factoryDependency, generatorDependency, () -> null);

        @SuppressWarnings("rawtypes")
        ValueDependency<SSOManager> managerDependency = new InjectedValueDependency<>(managerServiceName, SSOManager.class);
        ServiceName listenerServiceName = this.name.append("listener");
        CapabilityServiceBuilder<SessionListener> listenerBuilder = new SessionListenerBuilder(listenerServiceName, managerDependency);

        ValueDependency<SessionListener> listenerDependency = new InjectedValueDependency<>(listenerServiceName, SessionListener.class);
        ServiceName registryServiceName = this.name.append("registry");
        CapabilityServiceBuilder<SessionManagerRegistry> registryBuilder = new SessionManagerRegistryBuilder(registryServiceName, serverName, hostName, listenerDependency);

        this.manager = new InjectedValueDependency<>(managerBuilder, SSOManager.class);
        this.registry = new InjectedValueDependency<>(registryBuilder, SessionManagerRegistry.class);

        this.builders = Arrays.asList(factoryBuilder, generatorBuilder, managerBuilder, listenerBuilder, registryBuilder);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<SingleSignOnManager> configure(CapabilityServiceSupport support) {
        this.builders.forEach(builder -> builder.configure(support));
        return this;
    }

    @Override
    public ServiceBuilder<SingleSignOnManager> build(ServiceTarget target) {
        this.builders.forEach(builder -> builder.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install());
        Value<SingleSignOnManager> manager = () -> new DistributableSingleSignOnManager(this.manager.getValue(), this.registry.getValue());
        ServiceBuilder<SingleSignOnManager> builder = target.addService(this.name, new ValueService<>(manager)).setInitialMode(ServiceController.Mode.ON_DEMAND);
        Stream.of(this.manager, this.registry).forEach(dependency -> dependency.register(builder));
        return builder;
    }
}
