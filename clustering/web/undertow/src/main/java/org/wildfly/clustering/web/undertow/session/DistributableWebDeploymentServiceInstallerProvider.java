/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import jakarta.servlet.ServletContext;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.util.SavedRequest;

import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.modules.Module;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.web.container.WebDeploymentServiceInstallerProvider;
import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.service.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.service.session.LegacyDistributableSessionManagementProviderFactory;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;
import org.wildfly.elytron.web.undertow.server.servlet.ServletSecurityContextImpl.IdentityContainer;
import org.wildfly.extension.undertow.session.SessionAffinityProvider;
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * {@link SessionManagementProvider} for Undertow.
 * @author Paul Ferraro
 */
@MetaInfServices(WebDeploymentServiceInstallerProvider.class)
public class DistributableWebDeploymentServiceInstallerProvider implements WebDeploymentServiceInstallerProvider {

    private final LegacyDistributableSessionManagementProviderFactory legacyFactory = ServiceLoader.load(LegacyDistributableSessionManagementProviderFactory.class, LegacyDistributableSessionManagementProviderFactory.class.getClassLoader()).findFirst().orElseThrow();

    @Override
    public DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration configuration) {
        DeploymentUnit unit = configuration.getDeploymentUnit();
        DistributableSessionManagementProvider provider = this.findDistributableSessionManagementProvider(unit);
        Module module = unit.getAttachment(Attachments.MODULE);
        List<String> immutableClassNames = unit.getAttachmentList(DistributableSessionManagementProvider.IMMUTABILITY_ATTACHMENT_KEY);
        List<Class<?>> immutableClasses = new ArrayList<>(immutableClassNames.size());
        try {
            for (String immutableClassName : immutableClassNames) {
                immutableClasses.add(module.getClassLoader().loadClass(immutableClassName));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        Immutability immutability = Immutability.classes(immutableClasses);
        DeploymentServiceInstaller providedInstaller = provider.getSessionManagerFactoryServiceInstaller(new SessionManagerFactoryConfigurationAdapter<>(configuration, provider.getSessionManagementConfiguration(), immutability));

        ServiceDependency<SuspendableActivityRegistry> activityRegistry = ServiceDependency.on(SuspendableActivityRegistry.SERVICE_DESCRIPTOR);
        ServiceDependency<Executor> executor = ServiceDependency.on(Capabilities.MANAGEMENT_EXECUTOR);
        ServiceDependency<io.undertow.servlet.api.SessionManagerFactory> factory = ServiceDependency.<SessionManagerFactory<ServletContext, Map<String, Object>>>on(WebDeploymentServiceDescriptor.SESSION_MANAGER_FACTORY.resolve(unit)).map(new Function<>() {
            @Override
            public io.undertow.servlet.api.SessionManagerFactory apply(SessionManagerFactory<ServletContext, Map<String, Object>> factory) {
                return new DistributableSessionManagerFactory(factory, configuration) {
                    @Override
                    public UndertowSessionManager createSessionManager(Deployment deployment) {
                        return new SuspendableSessionManager(super.createSessionManager(deployment), activityRegistry.get(), executor.get());
                    }
                };
            }
        });
        DeploymentServiceInstaller installer = ServiceInstaller.builder(factory)
                .provides(org.wildfly.extension.undertow.deployment.WebDeploymentServiceDescriptor.SESSION_MANAGER_FACTORY.resolve(unit))
                .startWhen(StartWhen.REQUIRED)
                .requires(List.of(activityRegistry, executor))
                .build();

        return DeploymentServiceInstaller.combine(providedInstaller, installer);
    }

    @Override
    public DeploymentServiceInstaller getSessionAffinityProviderServiceInstaller(org.wildfly.clustering.web.container.WebDeploymentConfiguration configuration) {
        DeploymentUnit unit = configuration.getDeploymentUnit();
        DistributableSessionManagementProvider provider = this.findDistributableSessionManagementProvider(unit);

        DeploymentServiceInstaller locatorInstaller = provider.getRouteLocatorServiceInstaller(new WebDeploymentConfigurationAdapter(configuration));
        ServiceDependency<UnaryOperator<String>> locator = ServiceDependency.on(WebDeploymentServiceDescriptor.ROUTE_LOCATOR.resolve(configuration.getDeploymentUnit()));
        Supplier<SessionAffinityProvider> factory = locator.map(SessionAffinityProviderAdapter::new);
        DeploymentServiceInstaller affinityInstaller = ServiceInstaller.builder(factory)
                .provides(org.wildfly.extension.undertow.deployment.WebDeploymentServiceDescriptor.SESSION_AFFINITY_PROVIDER.resolve(unit))
                .requires(locator)
                .build();
        return DeploymentServiceInstaller.combine(locatorInstaller, affinityInstaller);
    }

    private DistributableSessionManagementProvider findDistributableSessionManagementProvider(DeploymentUnit unit) {
        DistributableSessionManagementProvider provider = unit.getAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
        // If invoked by an EAR deployment, there will be no WarMetaData attachment
        ReplicationConfig replicationConfig = Optional.ofNullable(unit.getAttachment(WarMetaData.ATTACHMENT_KEY)).map(WarMetaData::getMergedJBossWebMetaData).map(JBossWebMetaData::getReplicationConfig).orElse(null);
        // For compatibility, honor legacy <replication-config/> over an attached provider
        if ((replicationConfig != null) || (provider == null)) {
            if (provider != null) {
                UndertowClusteringLogger.ROOT_LOGGER.legacySessionManagementProviderOverride(unit.getName());
            } else {
                UndertowClusteringLogger.ROOT_LOGGER.legacySessionManagementProviderInUse(unit.getName());
            }
            // Fabricate DistributableSessionManagementProvider from legacy ReplicationConfig
            return this.legacyFactory.createSessionManagerProvider(unit, replicationConfig);
        }
        return provider;
    }

    static class WebDeploymentConfigurationAdapter implements WebDeploymentConfiguration {

        private final org.wildfly.clustering.web.container.WebDeploymentConfiguration configuration;

        public WebDeploymentConfigurationAdapter(org.wildfly.clustering.web.container.WebDeploymentConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public String getServerName() {
            return this.configuration.getServerName();
        }

        @Override
        public DeploymentUnit getDeploymentUnit() {
            return this.configuration.getDeploymentUnit();
        }
    }

    static class SessionManagerFactoryConfigurationAdapter<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> extends WebDeploymentConfigurationAdapter implements SessionManagerFactoryConfiguration<Map<String, Object>> {

        private final OptionalInt maxActiveSessions;
        private final ByteBufferMarshaller marshaller;
        private final Immutability immutability;
        private final SessionAttributePersistenceStrategy attributePersistenceStrategy;

        public SessionManagerFactoryConfigurationAdapter(org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration configuration, C managementConfiguration, Immutability immutability) {
            super(configuration);
            this.maxActiveSessions = configuration.getMaxActiveSessions();
            DeploymentUnit unit = configuration.getDeploymentUnit();
            Module module = unit.getAttachment(Attachments.MODULE);
            this.marshaller = managementConfiguration.getMarshallerFactory().apply(configuration.getDeploymentUnit());
            List<Immutability> loadedImmutabilities = new LinkedList<>();
            for (Immutability loadedImmutability : module.loadService(Immutability.class)) {
                loadedImmutabilities.add(loadedImmutability);
            }
            this.immutability = Immutability.composite(List.of(
                    Immutability.getDefault(),
                    Immutability.classes(List.of(AuthenticatedSession.class, SavedRequest.class, CachedIdentity.class, IdentityContainer.class)),
                    Immutability.composite(loadedImmutabilities),
                    immutability));
            this.attributePersistenceStrategy = managementConfiguration.getAttributePersistenceStrategy();
        }

        @Override
        public OptionalInt getMaxSize() {
            return this.maxActiveSessions;
        }

        @Override
        public ByteBufferMarshaller getMarshaller() {
            return this.marshaller;
        }

        @Override
        public Supplier<Map<String, Object>> getSessionContextFactory() {
            return ConcurrentHashMap::new;
        }

        @Override
        public Immutability getImmutability() {
            return this.immutability;
        }

        @Override
        public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
            return this.attributePersistenceStrategy;
        }
    }

    static class SessionAffinityProviderAdapter implements SessionAffinityProvider {

        private final UnaryOperator<String> locator;

        SessionAffinityProviderAdapter(UnaryOperator<String> locator) {
            this.locator = locator;
        }

        @Override
        public Optional<String> getAffinity(String sessionId) {
            return Optional.ofNullable(this.locator.apply(sessionId));
        }
    }
}
