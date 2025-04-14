/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleSessionAffinityProvider;
import org.jboss.as.web.session.SimpleSessionIdentifierCodec;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.container.WebDeploymentConfiguration;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.undertow.CookieConfig;
import org.wildfly.extension.undertow.Server;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import io.undertow.servlet.api.SessionConfigWrapper;
import io.undertow.servlet.api.SessionManagerFactory;

/**
 * {@link SessionManagementProvider} for non-distributed web deployments.
 * @author Paul Ferraro
 */
public class NonDistributableSessionManagementProvider implements SessionManagementProvider {
    private final Function<SessionManagerFactoryConfiguration, SessionManagerFactory> factory;

    public NonDistributableSessionManagementProvider(Function<SessionManagerFactoryConfiguration, SessionManagerFactory> factory) {
        this.factory = factory;
    }

    @Override
    public DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(ServiceName name, SessionManagerFactoryConfiguration configuration) {
        Supplier<SessionManagerFactory> provider = () -> this.factory.apply(configuration);
        return ServiceInstaller.builder(provider).provides(name).build();
    }

    @Override
    public DeploymentServiceInstaller getSessionAffinityServiceInstaller(DeploymentPhaseContext context, ServiceName name, WebDeploymentConfiguration configuration) {
        ServiceDependency<Server> server = ServiceDependency.on(Server.SERVICE_DESCRIPTOR, configuration.getServerName());
        Function<CookieConfig, SessionConfigWrapper> wrapperFactory = new Function<>() {
            @Override
            public SessionConfigWrapper apply(CookieConfig config) {
                String route = server.get().getRoute();
                SessionIdentifierCodec codec = new SimpleSessionIdentifierCodec(route);
                return (config != null) ? new AffinitySessionConfigWrapper(config, new SimpleSessionAffinityProvider(route)) : new CodecSessionConfigWrapper(codec);
            }
        };
        return ServiceInstaller.builder(Functions.constantSupplier(wrapperFactory)).provides(name).requires(server).build();
    }
}
