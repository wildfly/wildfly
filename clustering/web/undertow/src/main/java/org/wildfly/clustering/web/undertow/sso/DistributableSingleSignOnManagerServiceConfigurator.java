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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.CascadeRemovalLifecycleListener;
import org.wildfly.clustering.service.ChildTargetService;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.WebDefaultProviderRequirement;
import org.wildfly.clustering.web.WebProviderRequirement;
import org.wildfly.clustering.web.container.HostSingleSignOnManagementConfiguration;
import org.wildfly.clustering.web.sso.DistributableSSOManagementProvider;
import org.wildfly.clustering.web.sso.LegacySSOManagementProviderFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.undertow.UndertowBinaryRequirement;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.impl.SingleSignOnManager;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.server.session.SessionListener;


/**
 * Builds a distributable {@link SingleSignOnManagerFactory} service.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<SingleSignOnManager> {

    private final HostSingleSignOnManagementConfiguration configuration;
    private final LegacySSOManagementProviderFactory legacyProviderFactory;

    private volatile SupplierDependency<SSOManager<AuthenticatedSession, String, String, Void, Batch>> manager;
    private volatile SupplierDependency<SessionManagerRegistry> registry;

    private volatile SupplierDependency<DistributableSSOManagementProvider> provider;
    private volatile Consumer<ServiceTarget> installer;

    public DistributableSingleSignOnManagerServiceConfigurator(ServiceName name, HostSingleSignOnManagementConfiguration configuration, LegacySSOManagementProviderFactory legacyProviderFactory) {
        super(name);
        this.configuration = configuration;
        this.legacyProviderFactory = legacyProviderFactory;
    }

    @Override
    public SingleSignOnManager get() {
        return new DistributableSingleSignOnManager(this.manager.get(), this.registry.get());
    }

    @Override
    public ServiceConfigurator configure(OperationContext context) {
        String serverName = this.configuration.getServerName();
        String hostName = this.configuration.getHostName();
        CapabilityServiceSupport support = context.getCapabilityServiceSupport();
        SupplierDependency<DistributableSSOManagementProvider> provider = getProvider(context, serverName, hostName);
        ServiceName serviceName = this.getServiceName();
        ServiceName generatorServiceName = serviceName.append("generator");
        ServiceName managerServiceName = serviceName.append("manager");
        ServiceName listenerServiceName = serviceName.append("listener");
        ServiceName registryServiceName = serviceName.append("registry");
        this.manager = new ServiceSupplierDependency<>(managerServiceName);
        this.registry = new ServiceSupplierDependency<>(registryServiceName);
        this.provider = provider;
        this.installer = new Consumer<ServiceTarget>() {
            @Override
            public void accept(ServiceTarget target) {
                ServiceConfigurator factoryConfigurator = provider.get().getServiceConfigurator(hostName).configure(support);
                factoryConfigurator.build(target).install();

                new SessionIdGeneratorServiceConfigurator(generatorServiceName, serverName).configure(support).build(target).install();

                SupplierDependency<SSOManagerFactory<AuthenticatedSession, String, String, Batch>> factoryDependency = new ServiceSupplierDependency<>(factoryConfigurator);
                SupplierDependency<SessionIdGenerator> generatorDependency = new ServiceSupplierDependency<>(generatorServiceName);
                new SSOManagerServiceConfigurator<>(managerServiceName, factoryDependency, generatorDependency, () -> null).configure(support).build(target).install();

                SupplierDependency<SSOManager<AuthenticatedSession, String, String, Void, Batch>> managerDependency = new ServiceSupplierDependency<>(managerServiceName);
                new SessionListenerServiceConfigurator(listenerServiceName, managerDependency).configure(support).build(target).install();

                SupplierDependency<SessionListener> listenerDependency = new ServiceSupplierDependency<>(listenerServiceName);
                new SessionManagerRegistryServiceConfigurator(registryServiceName, serverName, hostName, listenerDependency).configure(support).build(target).install();
            }
        };
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceController<?> installerController = this.provider.register(target.addService(name.append("installer"))).setInstance(new ChildTargetService(this.installer)).install();

        ServiceBuilder<?> builder = target.addService(name).addListener(new CascadeRemovalLifecycleListener(installerController));
        Consumer<SingleSignOnManager> manager = new CompositeDependency(this.manager, this.registry).register(builder).provides(name);
        Service service = new FunctionalService<>(manager, Function.identity(), this);
        return builder.setInstance(service);
    }

    private SupplierDependency<DistributableSSOManagementProvider> getProvider(OperationContext context, String serverName, String hostName) {
        String hostCapabilityName = UndertowBinaryRequirement.HOST.resolve(serverName, hostName);
        if (context.hasOptionalCapability(WebProviderRequirement.SSO_MANAGEMENT_PROVIDER.resolve(hostName), hostCapabilityName, null)) {
            return new ServiceSupplierDependency<>(WebProviderRequirement.SSO_MANAGEMENT_PROVIDER.getServiceName(context, hostName));
        } else if (context.hasOptionalCapability(WebDefaultProviderRequirement.SSO_MANAGEMENT_PROVIDER.getName(), hostCapabilityName, null)) {
            return new ServiceSupplierDependency<>(WebDefaultProviderRequirement.SSO_MANAGEMENT_PROVIDER.getServiceName(context));
        }
        UndertowClusteringLogger.ROOT_LOGGER.legacySingleSignOnProviderInUse(hostName);
        return new SimpleSupplierDependency<>(this.legacyProviderFactory.createSSOManagementProvider());
    }
}
