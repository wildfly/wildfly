/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.sso.elytron;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.ImmediateValueDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.MappedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilderProvider;
import org.wildfly.clustering.web.undertow.sso.SSOManagerBuilder;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerBuilder implements CapabilityServiceBuilder<SingleSignOnManager>, Function<SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch>, SingleSignOnManager> {

    private final ServiceName name;
    private final ValueDependency<SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch>> manager;

    private final Collection<CapabilityServiceBuilder<?>> builders;

    @SuppressWarnings("unchecked")
    public DistributableSingleSignOnManagerBuilder(ServiceName name, String securityDomainName, SessionIdGenerator generator, SSOManagerFactoryBuilderProvider<Batch> provider) {
        this.name = name;

        CapabilityServiceBuilder<SSOManagerFactory<ElytronAuthentication, String, Map.Entry<String, URI>, Batch>> factoryBuilder = provider.<ElytronAuthentication, String, Map.Entry<String, URI>>getBuilder(securityDomainName);
        ValueDependency<SSOManagerFactory<ElytronAuthentication, String, Map.Entry<String, URI>, Batch>> factoryDependency = new InjectedValueDependency<>(factoryBuilder, (Class<SSOManagerFactory<ElytronAuthentication, String, Map.Entry<String, URI>, Batch>>) (Class<?>) SSOManagerFactory.class);
        ValueDependency<SessionIdGenerator> generatorDependency = new ImmediateValueDependency<>(generator);
        ServiceName managerServiceName = this.name.append("manager");
        CapabilityServiceBuilder<SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch>> managerBuilder = new SSOManagerBuilder<>(managerServiceName, factoryDependency, generatorDependency, new LocalSSOContextFactory());

        this.manager = new InjectedValueDependency<>(managerServiceName, (Class<SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch>>) (Class<?>) SSOManager.class);

        this.builders = Arrays.asList(factoryBuilder, managerBuilder);
    }

    @Override
    public SingleSignOnManager apply(SSOManager<ElytronAuthentication, String, Entry<String, URI>, LocalSSOContext, Batch> manager) {
        return new DistributableSingleSignOnManager(manager);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<SingleSignOnManager> configure(CapabilityServiceSupport support) {
        for (CapabilityServiceBuilder<?> builder : this.builders) {
            builder.configure(support);
        }
        return this;
    }

    @Override
    public ServiceBuilder<SingleSignOnManager> build(ServiceTarget target) {
        for (CapabilityServiceBuilder<?> builder : this.builders) {
            builder.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        }
        Service<SingleSignOnManager> service = new MappedValueService<>(this, this.manager);
        return this.manager.register(target.addService(this.name, service)).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
