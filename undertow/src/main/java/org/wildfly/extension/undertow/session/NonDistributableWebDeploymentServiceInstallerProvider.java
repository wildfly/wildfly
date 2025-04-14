/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.clustering.web.container.WebDeploymentServiceInstallerProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.container.WebDeploymentConfiguration;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.deployment.WebDeploymentServiceDescriptor;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import io.undertow.servlet.api.SessionManagerFactory;

/**
 * {@link WebDeploymentServiceInstallerProvider} for non-distributed web deployments.
 * @author Paul Ferraro
 */
public class NonDistributableWebDeploymentServiceInstallerProvider implements WebDeploymentServiceInstallerProvider, Supplier<WebDeploymentServiceInstallerProvider> {
    private final Function<SessionManagerFactoryConfiguration, SessionManagerFactory> factory;

    public NonDistributableWebDeploymentServiceInstallerProvider(Function<SessionManagerFactoryConfiguration, SessionManagerFactory> factory) {
        this.factory = factory;
    }

    @Override
    public DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(SessionManagerFactoryConfiguration configuration) {
        Supplier<SessionManagerFactory> provider = () -> this.factory.apply(configuration);
        return ServiceInstaller.builder(provider).provides(WebDeploymentServiceDescriptor.SESSION_MANAGER_FACTORY.resolve(configuration.getDeploymentUnit())).build();
    }

    @Override
    public DeploymentServiceInstaller getSessionAffinityProviderServiceInstaller(WebDeploymentConfiguration configuration) {
        ServiceDependency<Server> server = ServiceDependency.on(Server.SERVICE_DESCRIPTOR, configuration.getServerName());
        return ServiceInstaller.builder(server.map(NonDistributableSessionAffinityProvider::new))
                .provides(WebDeploymentServiceDescriptor.SESSION_AFFINITY_PROVIDER.resolve(configuration.getDeploymentUnit()))
                .build();
    }

    @Override
    public WebDeploymentServiceInstallerProvider get() {
        // Supplies distributable implementation, when no distributable implementation is available.
        UndertowLogger.ROOT_LOGGER.clusteringNotSupported();
        return this;
    }
}
