/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.user.elytron;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.Services;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceController;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.protostream.modules.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.session.user.UserManager;
import org.wildfly.clustering.session.user.UserManagerConfiguration;
import org.wildfly.clustering.session.user.UserManagerFactory;
import org.wildfly.clustering.web.container.SingleSignOnManagerConfiguration;
import org.wildfly.clustering.web.container.SingleSignOnManagerServiceInstallerProvider;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.clustering.web.service.user.LegacyDistributableUserManagementProviderFactory;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * {@link org.wildfly.extension.undertow.session.SessionManagementProviderFactory} for Undertow using either the {@link org.wildfly.clustering.web.DistributableUserManagementProvider.DistributableSSOManagementProvider} for the given security domain, the default provider, or a legacy provider.
 * @author Paul Ferraro
 */
@MetaInfServices(SingleSignOnManagerServiceInstallerProvider.class)
public class DistributableSingleSignOnManagerServiceInstallerProvider implements SingleSignOnManagerServiceInstallerProvider {

    private final LegacyDistributableUserManagementProviderFactory legacyProviderFactory = ServiceLoader.load(LegacyDistributableUserManagementProviderFactory.class, LegacyDistributableUserManagementProviderFactory.class.getClassLoader()).findFirst().orElseThrow();

    @Override
    public ResourceServiceInstaller getServiceInstaller(SingleSignOnManagerConfiguration configuration) {
        String securityDomainName = configuration.getSecurityDomainName();
        ResourceServiceInstaller providerInstaller = new ResourceServiceInstaller() {
            @Override
            public Consumer<OperationContext> install(OperationContext context) {
                ServiceDependency<DistributableUserManagementProvider> provider = DistributableSingleSignOnManagerServiceInstallerProvider.this.getUserManagementProvider(context, securityDomainName);
                return ServiceInstaller.builder(new ServiceInstaller() {
                    @Override
                    public ServiceController<?> install(RequirementServiceTarget target) {
                        for (ServiceInstaller installer : provider.get().getServiceInstallers(securityDomainName)) {
                            installer.install(target);
                        }
                        return null;
                    }
                }, context.getCapabilityServiceSupport()).requires(provider).build().install(context);
            }
        };

        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        ServiceDependency<UserManagerFactory<CachedIdentity, String, Map.Entry<String, URI>>> userManagerFactory = ServiceDependency.on(DistributableUserManagementProvider.USER_MANAGER_FACTORY, securityDomainName);
        UserManagerConfiguration<AtomicReference<SecurityIdentity>> userManagerConfiguration = new UserManagerConfiguration<>() {
            @Override
            public Supplier<String> getIdentifierFactory() {
                return configuration.getIdentifierGenerator()::get;
            }

            @Override
            public ByteBufferMarshaller getMarshaller() {
                ClassLoader classLoader = WildFlySecurityManager.getClassLoaderPrivileged(this.getClass());
                return new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(new ModuleClassLoaderMarshaller(loader.get())).load(classLoader).build());
            }

            @Override
            public Supplier<AtomicReference<SecurityIdentity>> getTransientContextFactory() {
                return AtomicReference::new;
            }
        };
        Supplier<UserManager<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>>> factory = new Supplier<>() {
            @Override
            public UserManager<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> get() {
                return userManagerFactory.get().createUserManager(userManagerConfiguration);
            }
        };
        ResourceServiceInstaller userManagerInstaller = ServiceInstaller.builder(DistributableSingleSignOnManager::new, factory)
                .provides(ServiceNameFactory.resolveServiceName(SingleSignOnManagerServiceInstallerProvider.SINGLE_SIGN_ON_MANAGER, securityDomainName))
                .requires(List.of(loader, userManagerFactory))
                .onStart(UserManager::start)
                .onStop(UserManager::stop)
                .build();

        return ResourceServiceInstaller.combine(providerInstaller, userManagerInstaller);
    }

    ServiceDependency<DistributableUserManagementProvider> getUserManagementProvider(OperationContext context, String securityDomainName) {
        String securityDomainCapabilityName = RuntimeCapability.buildDynamicCapabilityName("org.wildfly.undertow.application-security-domain", securityDomainName);
        if (context.hasOptionalCapability(RuntimeCapability.resolveCapabilityName(DistributableUserManagementProvider.SERVICE_DESCRIPTOR, securityDomainName), securityDomainCapabilityName, null)) {
            return ServiceDependency.on(DistributableUserManagementProvider.SERVICE_DESCRIPTOR, securityDomainName);
        } else if (context.hasOptionalCapability(DistributableUserManagementProvider.DEFAULT_SERVICE_DESCRIPTOR.getName(), securityDomainCapabilityName, null)) {
            return ServiceDependency.on(DistributableUserManagementProvider.DEFAULT_SERVICE_DESCRIPTOR);
        }
        UndertowClusteringLogger.ROOT_LOGGER.legacySingleSignOnProviderInUse(securityDomainName);
        return ServiceDependency.of(this.legacyProviderFactory.createUserManagementProvider());
    }
}
