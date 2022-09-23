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
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.CascadeRemovalLifecycleListener;
import org.wildfly.clustering.service.ChildTargetService;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.container.SecurityDomainSingleSignOnManagementConfiguration;
import org.wildfly.clustering.web.service.WebDefaultProviderRequirement;
import org.wildfly.clustering.web.service.WebProviderRequirement;
import org.wildfly.clustering.web.service.sso.DistributableSSOManagementProvider;
import org.wildfly.clustering.web.service.sso.LegacySSOManagementProviderFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;
import org.wildfly.clustering.web.undertow.sso.SSOManagerServiceConfigurator;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<SSOManager<CachedIdentity, String, Map.Entry<String, URI>, LocalSSOContext, Batch>, SingleSignOnManager> {

    private final SecurityDomainSingleSignOnManagementConfiguration configuration;
    private final LegacySSOManagementProviderFactory legacyProviderFactory;

    private volatile SupplierDependency<DistributableSSOManagementProvider> provider;
    private volatile SupplierDependency<SSOManager<CachedIdentity, String, Map.Entry<String, URI>, LocalSSOContext, Batch>> manager;
    private volatile Consumer<ServiceTarget> installer;

    public DistributableSingleSignOnManagerServiceConfigurator(ServiceName name, SecurityDomainSingleSignOnManagementConfiguration configuration, LegacySSOManagementProviderFactory legacyProviderFactory) {
        super(name);
        this.configuration = configuration;
        this.legacyProviderFactory = legacyProviderFactory;
    }

    @Override
    public SingleSignOnManager apply(SSOManager<CachedIdentity, String, Entry<String, URI>, LocalSSOContext, Batch> manager) {
        return new DistributableSingleSignOnManager(manager);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context) {
        String securityDomainName = this.configuration.getSecurityDomainName();
        Supplier<String> generator = this.configuration.getIdentifierGenerator();
        CapabilityServiceSupport support = context.getCapabilityServiceSupport();
        SupplierDependency<DistributableSSOManagementProvider> provider = getProvider(context, securityDomainName);
        ServiceName managerServiceName = this.getServiceName().append("manager");
        this.manager = new ServiceSupplierDependency<>(managerServiceName);
        this.provider = provider;
        this.installer = new Consumer<>() {
            @Override
            public void accept(ServiceTarget target) {
                ServiceConfigurator factoryConfigurator = provider.get().getServiceConfigurator(securityDomainName).configure(support);
                factoryConfigurator.build(target).install();

                SupplierDependency<SSOManagerFactory<CachedIdentity, String, Map.Entry<String, URI>, Batch>> factoryDependency = new ServiceSupplierDependency<>(factoryConfigurator);
                SupplierDependency<SessionIdGenerator> generatorDependency = new SimpleSupplierDependency<>(new SessionIdGeneratorAdapter(generator));
                new SSOManagerServiceConfigurator<>(managerServiceName, factoryDependency, generatorDependency, new LocalSSOContextFactory()).configure(support).build(target).install();
            }
        };
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceController<?> installerController = this.provider.register(target.addService(name.append("installer"))).setInstance(new ChildTargetService(this.installer)).install();

        ServiceBuilder<?> builder = target.addService(name).addListener(new CascadeRemovalLifecycleListener(installerController));
        Consumer<SingleSignOnManager> manager = this.manager.register(builder).provides(name);
        Service service = new FunctionalService<>(manager, this, this.manager);
        return builder.setInstance(service);
    }

    private SupplierDependency<DistributableSSOManagementProvider> getProvider(OperationContext context, String securityDomainName) {
        String securityDomainCapabilityName = RuntimeCapability.buildDynamicCapabilityName(Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN, securityDomainName);
        if (context.hasOptionalCapability(WebProviderRequirement.SSO_MANAGEMENT_PROVIDER.resolve(securityDomainName), securityDomainCapabilityName, null)) {
            return new ServiceSupplierDependency<>(WebProviderRequirement.SSO_MANAGEMENT_PROVIDER.getServiceName(context, securityDomainName));
        } else if (context.hasOptionalCapability(WebDefaultProviderRequirement.SSO_MANAGEMENT_PROVIDER.getName(), securityDomainCapabilityName, null)) {
            return new ServiceSupplierDependency<>(WebDefaultProviderRequirement.SSO_MANAGEMENT_PROVIDER.getServiceName(context));
        }
        UndertowClusteringLogger.ROOT_LOGGER.legacySingleSignOnProviderInUse(securityDomainName);
        return new SimpleSupplierDependency<>(this.legacyProviderFactory.createSSOManagementProvider());
    }
}
