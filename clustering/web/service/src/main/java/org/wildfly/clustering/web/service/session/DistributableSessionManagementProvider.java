/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.session;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;

/**
 * A distributable session management provider.
 * @author Paul Ferraro
 */
public interface DistributableSessionManagementProvider<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> {
    AttachmentKey<DistributableSessionManagementProvider<DistributableSessionManagementConfiguration<DeploymentUnit>>> ATTACHMENT_KEY = AttachmentKey.create(DistributableSessionManagementProvider.class);
    AttachmentKey<AttachmentList<String>> IMMUTABILITY_ATTACHMENT_KEY = AttachmentKey.createList(String.class);

    /**
     * Returns a {@link CapabilityServiceConfigurator} used to configure a service providing a {@link org.wildfly.clustering.web.session.SessionManagerFactory}.
     * @param <S> the HttpSession specification type
     * @param <SC> the ServletContext specification type
     * @param <AL> the HttpSessionAttributeListener specification type
     * @param <LC> the local context type
     * @param configuration the configuration of the session manager factory
     * @return a service configurator
     */
    <S, SC, AL, LC> CapabilityServiceConfigurator getSessionManagerFactoryServiceConfigurator(SessionManagerFactoryConfiguration<S, SC, AL, LC> configuration);

    /**
     * Returns a {@link CapabilityServiceConfigurator} used to configure a service providing a {@link org.wildfly.clustering.web.routing.RouteLocator}.
     * @param configuration the configuration of a deployment
     * @return a service configurator
     */
    CapabilityServiceConfigurator getRouteLocatorServiceConfigurator(WebDeploymentConfiguration configuration);

    C getSessionManagementConfiguration();
}
