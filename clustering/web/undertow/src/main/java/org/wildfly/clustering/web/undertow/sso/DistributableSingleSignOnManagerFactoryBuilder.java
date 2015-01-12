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

import java.util.Arrays;
import java.util.ServiceLoader;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilderProvider;
import org.wildfly.extension.undertow.security.sso.SingleSignOnManagerFactory;


/**
 * Builds a distributable {@link SingleSignOnManagerFactory} service.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerFactoryBuilder implements org.wildfly.extension.undertow.security.sso.DistributableSingleSignOnManagerFactoryBuilder, Value<SingleSignOnManagerFactory> {

    private static SSOManagerFactoryBuilderProvider<Batch> load() {
        for (SSOManagerFactoryBuilderProvider<Batch> builder: ServiceLoader.load(SSOManagerFactoryBuilderProvider.class, SSOManagerFactoryBuilderProvider.class.getClassLoader())) {
            return builder;
        }
        return null;
    }

    private final SSOManagerFactoryBuilderProvider<Batch> provider;
    @SuppressWarnings("rawtypes")
    private final InjectedValue<SSOManagerFactory> manager = new InjectedValue<>();
    private final InjectedValue<SessionManagerRegistry> registry = new InjectedValue<>();

    public DistributableSingleSignOnManagerFactoryBuilder() {
        this(load());
    }

    private DistributableSingleSignOnManagerFactoryBuilder(SSOManagerFactoryBuilderProvider<Batch> builder) {
        this.provider = builder;
    }

    @Override
    public ServiceBuilder<SingleSignOnManagerFactory> build(ServiceTarget target, ServiceName name, String serverName, String hostName) {
        Builder<SSOManagerFactory<AuthenticatedSession, String, Batch>> factoryBuilder = this.provider.getBuilder(hostName);
        Builder<SessionManagerRegistry> registryBuilder = new SessionManagerRegistryBuilder(serverName, hostName);
        for (Builder<?> builder : Arrays.asList(factoryBuilder, registryBuilder)) {
            builder.build(target).install();
        }
        return target.addService(name, new ValueService<>(this))
                .addDependency(factoryBuilder.getServiceName(), SSOManagerFactory.class, this.manager)
                .addDependency(registryBuilder.getServiceName(), SessionManagerRegistry.class, this.registry)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SingleSignOnManagerFactory getValue() {
        return new DistributableSingleSignOnManagerFactory(this.manager.getValue(), this.registry.getValue());
    }
}
