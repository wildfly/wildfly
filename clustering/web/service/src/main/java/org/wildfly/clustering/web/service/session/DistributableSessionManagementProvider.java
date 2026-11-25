/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.session;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;

/**
 * A distributable session management provider.
 * @author Paul Ferraro
 */
public interface DistributableSessionManagementProvider {
    NullaryServiceDescriptor<DistributableSessionManagementProvider> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.web.default-session-management-provider", DistributableSessionManagementProvider.class);
    UnaryServiceDescriptor<DistributableSessionManagementProvider> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.web.session-management-provider", DEFAULT_SERVICE_DESCRIPTOR);

    AttachmentKey<DistributableSessionManagementProvider> ATTACHMENT_KEY = AttachmentKey.create(DistributableSessionManagementProvider.class);
    AttachmentKey<AttachmentList<String>> IMMUTABILITY_ATTACHMENT_KEY = AttachmentKey.createList(String.class);

    /**
     * Returns a {@link CapabilityServiceConfigurator} used to configure a service providing a {@link org.wildfly.clustering.web.session.SessionManagerFactory}.
     * @param <C> the session context type
     * @param configuration the configuration of the session manager factory
     * @return a service configurator
     */
    <C> DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(SessionManagerFactoryConfiguration<C> configuration);

    /**
     * Returns a {@link CapabilityServiceConfigurator} used to configure a service providing a {@link org.wildfly.clustering.web.routing.RouteLocator}.
     * @param configuration the configuration of a deployment
     * @return a service configurator
     */
    DeploymentServiceInstaller getRouteLocatorServiceInstaller(WebDeploymentConfiguration configuration);

    DistributableSessionManagementConfiguration<DeploymentUnit> getSessionManagementConfiguration();
}
