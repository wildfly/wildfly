/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.function.Function;

import io.undertow.server.session.InMemorySessionManager;
import io.undertow.servlet.api.SessionManagerFactory;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.msc.service.ServiceName;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.session.NonDistributableSessionManagementProvider;
import org.wildfly.extension.undertow.session.SessionManagementProviderFactory;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerDeploymentProcessor implements DeploymentUnitProcessor, Function<SessionManagerFactoryConfiguration, SessionManagerFactory> {
    private final String defaultServerName;
    private final SessionManagementProviderFactory sessionManagementProviderFactory;
    private final SessionManagementProvider nonDistributableSessionManagementProvider;

    public SharedSessionManagerDeploymentProcessor(String defaultServerName) {
        this.defaultServerName = defaultServerName;
        Iterator<SessionManagementProviderFactory> factories = ServiceLoader.load(SessionManagementProviderFactory.class, SessionManagementProviderFactory.class.getClassLoader()).iterator();
        this.sessionManagementProviderFactory = factories.hasNext() ? factories.next() : null;
        this.nonDistributableSessionManagementProvider = new NonDistributableSessionManagementProvider(this);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        SharedSessionManagerConfig sharedConfig = deploymentUnit.getAttachment(SharedSessionManagerConfig.ATTACHMENT_KEY);
        if (sharedConfig == null) return;

        String deploymentName = (deploymentUnit.getParent() == null) ? deploymentUnit.getName() : String.join(".", deploymentUnit.getParent().getName(), deploymentUnit.getName());
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        String serverName = Optional.ofNullable(warMetaData).map(metaData -> metaData.getMergedJBossWebMetaData().getServerInstanceName())
                .orElse(Optional.ofNullable(DefaultDeploymentMappingProvider.instance().getMapping(deploymentName)).map(Map.Entry::getKey).orElse(this.defaultServerName));
        SessionConfigMetaData sessionConfig = sharedConfig.getSessionConfig();
        ServletContainerService servletContainer = deploymentUnit.getAttachment(UndertowAttachments.SERVLET_CONTAINER_SERVICE);
        Integer defaultSessionTimeout = ((sessionConfig != null) && sessionConfig.getSessionTimeoutSet()) ? sessionConfig.getSessionTimeout() : (servletContainer != null) ? servletContainer.getDefaultSessionTimeout() : Integer.valueOf(30);

        ServiceName deploymentServiceName = deploymentUnit.getServiceName();

        ServiceName managerServiceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_MANAGER_SERVICE_NAME);
        ServiceName affinityServiceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_AFFINITY_SERVICE_NAME);

        SessionManagementProvider provider = this.getDistributableWebDeploymentProvider(phaseContext, sharedConfig);
        SessionManagerFactoryConfiguration configuration = new SessionManagerFactoryConfiguration() {
            @Override
            public String getServerName() {
                return serverName;
            }

            @Override
            public String getDeploymentName() {
                return deploymentName;
            }

            @Override
            public OptionalInt getMaxActiveSessions() {
                Integer maxActiveSessions = sharedConfig.getMaxActiveSessions();
                return ((maxActiveSessions != null) && maxActiveSessions > 0) ? OptionalInt.of(maxActiveSessions) : OptionalInt.empty();
            }

            @Override
            public DeploymentUnit getDeploymentUnit() {
                return deploymentUnit;
            }

            @Override
            public Duration getDefaultSessionTimeout() {
                return Duration.ofMinutes(defaultSessionTimeout);
            }
        };
        provider.getSessionManagerFactoryServiceInstaller(managerServiceName, configuration).install(phaseContext);
        provider.getSessionAffinityServiceInstaller(phaseContext, affinityServiceName, configuration).install(phaseContext);
    }

    @SuppressWarnings("deprecation")
    private SessionManagementProvider getDistributableWebDeploymentProvider(DeploymentPhaseContext context, SharedSessionManagerConfig config) {
        if (config.isDistributable()) {
            if (this.sessionManagementProviderFactory != null) {
                return this.sessionManagementProviderFactory.createSessionManagementProvider(context, config.getReplicationConfig());
            }
            // Fallback to non-distributable session manager if server does not support clustering
            UndertowLogger.ROOT_LOGGER.clusteringNotSupported();
        }
        return this.nonDistributableSessionManagementProvider;
    }

    @Override
    public SessionManagerFactory apply(SessionManagerFactoryConfiguration configuration) {
        String deploymentName = configuration.getDeploymentName();
        OptionalInt maxActiveSessions = configuration.getMaxActiveSessions();
        InMemorySessionManager manager = maxActiveSessions.isPresent() ? new InMemorySessionManager(deploymentName, maxActiveSessions.getAsInt()) : new InMemorySessionManager(deploymentName);
        manager.setDefaultSessionTimeout((int) configuration.getDefaultSessionTimeout().getSeconds());
        return new ImmediateSessionManagerFactory(manager);
    }
}
