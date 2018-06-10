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
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryServiceConfiguratorProvider;
import org.wildfly.clustering.web.undertow.sso.SSOManagerServiceConfigurator;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch>, SingleSignOnManager> {

    private final SupplierDependency<SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch>> manager;

    private final Collection<CapabilityServiceConfigurator> configurators;

    public DistributableSingleSignOnManagerServiceConfigurator(ServiceName name, String securityDomainName, SessionIdGenerator generator, SSOManagerFactoryServiceConfiguratorProvider provider) {
        super(name);

        CapabilityServiceConfigurator factoryConfigurator = provider.getServiceConfigurator(securityDomainName);
        SupplierDependency<SSOManagerFactory<ElytronAuthentication, String, Map.Entry<String, URI>, Batch>> factoryDependency = new ServiceSupplierDependency<>(factoryConfigurator);
        SupplierDependency<SessionIdGenerator> generatorDependency = new SimpleSupplierDependency<>(generator);
        ServiceName managerServiceName = this.getServiceName().append("manager");
        CapabilityServiceConfigurator managerConfigurator = new SSOManagerServiceConfigurator<>(managerServiceName, factoryDependency, generatorDependency, new LocalSSOContextFactory());

        this.manager = new ServiceSupplierDependency<>(managerServiceName);

        this.configurators = Arrays.asList(factoryConfigurator, managerConfigurator);
    }

    @Override
    public SingleSignOnManager apply(SSOManager<ElytronAuthentication, String, Entry<String, URI>, LocalSSOContext, Batch> manager) {
        return new DistributableSingleSignOnManager(manager);
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
        Consumer<SingleSignOnManager> manager = this.manager.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(manager, this, this.manager);
        return builder.setInstance(service);
    }
}
