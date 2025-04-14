/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jakarta.servlet.ServletContext;

import io.undertow.servlet.api.SessionConfigWrapper;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.container.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.undertow.routing.DistributableSessionAffinityProvider;
import org.wildfly.clustering.web.undertow.routing.DistributableSessionIdentifierCodec;
import org.wildfly.extension.undertow.CookieConfig;
import org.wildfly.extension.undertow.session.AffinitySessionConfigWrapper;
import org.wildfly.extension.undertow.session.CodecSessionConfigWrapper;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * {@link SessionManagementProvider} for Undertow.
 * @author Paul Ferraro
 */
public class UndertowDistributableSessionManagementProvider implements SessionManagementProvider {

    private final DistributableSessionManagementProvider provider;
    private final Immutability immutability;

    public UndertowDistributableSessionManagementProvider(DistributableSessionManagementProvider provider, Immutability immutability) {
        this.provider = provider;
        this.immutability = immutability;
    }

    @Override
    public DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(ServiceName name, SessionManagerFactoryConfiguration configuration) {

        DeploymentServiceInstaller providedInstaller = this.provider.getSessionManagerFactoryServiceInstaller(new SessionManagerFactoryConfigurationAdapter<>(configuration, this.provider.getSessionManagementConfiguration(), this.immutability));

        Function<SessionManagerFactory<ServletContext, Map<String, Object>>, io.undertow.servlet.api.SessionManagerFactory> mapper = new Function<>() {
            @Override
            public io.undertow.servlet.api.SessionManagerFactory apply(SessionManagerFactory<ServletContext, Map<String, Object>> factory) {
                return new DistributableSessionManagerFactory(factory, configuration);
            }
        };
        DeploymentServiceInstaller installer = ServiceInstaller.builder(ServiceDependency.on(WebDeploymentServiceDescriptor.SESSION_MANAGER_FACTORY, configuration.getDeploymentName()).map(mapper)).provides(name).build();

        return DeploymentServiceInstaller.combine(providedInstaller, installer);
    }

    @Override
    public DeploymentServiceInstaller getSessionAffinityServiceInstaller(DeploymentPhaseContext context, ServiceName name, WebDeploymentConfiguration configuration) {
        DeploymentServiceInstaller locatorInstaller = this.provider.getRouteLocatorServiceInstaller(context, new WebDeploymentConfigurationAdapter(configuration));

        ServiceDependency<UnaryOperator<String>> locator = ServiceDependency.on(WebDeploymentServiceDescriptor.ROUTE_LOCATOR, configuration.getDeploymentName());
        Function<CookieConfig, SessionConfigWrapper> wrapperFactory = new Function<>() {
            @Override
            public SessionConfigWrapper apply(CookieConfig config) {
                UnaryOperator<String> routeLocator = locator.get();
                SessionIdentifierCodec codec = new DistributableSessionIdentifierCodec(routeLocator);
                return (config != null) ? new AffinitySessionConfigWrapper(config, new DistributableSessionAffinityProvider(routeLocator)) : new CodecSessionConfigWrapper(codec);
            }
        };
        DeploymentServiceInstaller wrapperFactoryInstaller = ServiceInstaller.builder(Supplier.of(wrapperFactory)).requires(locator).provides(name).build();

        return DeploymentServiceInstaller.combine(locatorInstaller, wrapperFactoryInstaller);
    }
}
