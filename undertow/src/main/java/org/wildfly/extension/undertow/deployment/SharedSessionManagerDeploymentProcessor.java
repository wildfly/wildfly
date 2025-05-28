/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.time.Duration;
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
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.wildfly.clustering.web.container.WebDeploymentServiceInstallerProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.session.NonDistributableWebDeploymentServiceInstallerProvider;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerDeploymentProcessor implements DeploymentUnitProcessor {
    private final String defaultServerName;
    private final Optional<WebDeploymentServiceInstallerProvider> distributableServiceInstallerProvider;
    private final NonDistributableWebDeploymentServiceInstallerProvider nonDistributableServiceInstallerProvider;

    public SharedSessionManagerDeploymentProcessor(String defaultServerName) {
        this.defaultServerName = defaultServerName;
        this.nonDistributableServiceInstallerProvider = new NonDistributableWebDeploymentServiceInstallerProvider(new Function<>() {
            @Override
            public SessionManagerFactory apply(SessionManagerFactoryConfiguration configuration) {
                String deploymentName = configuration.getDeploymentName();
                OptionalInt maxActiveSessions = configuration.getMaxActiveSessions();
                InMemorySessionManager manager = maxActiveSessions.isPresent() ? new InMemorySessionManager(deploymentName, maxActiveSessions.getAsInt()) : new InMemorySessionManager(deploymentName);
                manager.setDefaultSessionTimeout((int) configuration.getDefaultSessionTimeout().getSeconds());
                return new ImmediateSessionManagerFactory(manager);
            }
        });
        this.distributableServiceInstallerProvider = ServiceLoader.load(WebDeploymentServiceInstallerProvider.class, WebDeploymentServiceInstallerProvider.class.getClassLoader()).findFirst();
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

        WebDeploymentServiceInstallerProvider provider = sharedConfig.isDistributable() ? this.distributableServiceInstallerProvider.orElseGet(this.nonDistributableServiceInstallerProvider) : this.nonDistributableServiceInstallerProvider;
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
        provider.getSessionManagerFactoryServiceInstaller(configuration).install(phaseContext);
        provider.getSessionAffinityProviderServiceInstaller(configuration).install(phaseContext);
    }
}
