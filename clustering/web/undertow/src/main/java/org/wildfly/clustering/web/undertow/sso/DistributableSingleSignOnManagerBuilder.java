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
import io.undertow.security.impl.InMemorySingleSignOnManager;
import io.undertow.security.impl.SingleSignOnManager;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.server.session.SessionListener;

import java.util.Arrays;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilderProvider;
import org.wildfly.extension.undertow.UndertowService;


/**
 * Builds a distributable {@link SingleSignOnManagerFactory} service.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerBuilder implements org.wildfly.extension.undertow.security.sso.DistributableSingleSignOnManagerBuilder, Value<SingleSignOnManager> {

    @SuppressWarnings("rawtypes")
    private static final Optional<SSOManagerFactoryBuilderProvider> PROVIDER = StreamSupport.stream(ServiceLoader.load(SSOManagerFactoryBuilderProvider.class, SSOManagerFactoryBuilderProvider.class.getClassLoader()).spliterator(), false).findFirst();

    @SuppressWarnings("rawtypes")
    private final InjectedValue<SSOManager> manager = new InjectedValue<>();
    private final InjectedValue<SessionManagerRegistry> registry = new InjectedValue<>();

    @Override
    public ServiceBuilder<SingleSignOnManager> build(ServiceTarget target, ServiceName name, String serverName, String hostName) {
        if (!PROVIDER.isPresent()) {
            return target.addService(name, new ValueService<>(new ImmediateValue<>(new InMemorySingleSignOnManager())));
        }

        ServiceName hostServiceName = UndertowService.virtualHostName(serverName, hostName);

        SSOManagerFactoryBuilderProvider<Batch> provider = PROVIDER.get();
        Builder<SSOManagerFactory<AuthenticatedSession, String, Batch>> factoryBuilder = provider.getBuilder(hostName);
        Builder<SessionIdGenerator> generatorBuilder = new SessionIdGeneratorBuilder(hostServiceName);
        Builder<SSOManager<AuthenticatedSession, String, Void, Batch>> managerBuilder = new SSOManagerBuilder(factoryBuilder.getServiceName(), generatorBuilder.getServiceName());
        Builder<SessionListener> listenerBuilder = new SessionListenerBuilder(managerBuilder.getServiceName());
        Builder<SessionManagerRegistry> registryBuilder = new SessionManagerRegistryBuilder(hostServiceName, listenerBuilder.getServiceName());

        Arrays.asList(factoryBuilder, generatorBuilder, managerBuilder, listenerBuilder, registryBuilder).forEach(builder -> builder.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install());

        return target.addService(name, new ValueService<>(this))
                .addDependency(managerBuilder.getServiceName(), SSOManager.class, this.manager)
                .addDependency(registryBuilder.getServiceName(), SessionManagerRegistry.class, this.registry)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SingleSignOnManager getValue() {
        return new DistributableSingleSignOnManager(this.manager.getValue(), this.registry.getValue());
    }
}
