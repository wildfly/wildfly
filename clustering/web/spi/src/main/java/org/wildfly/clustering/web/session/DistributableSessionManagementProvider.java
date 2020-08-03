/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.session;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.wildfly.clustering.web.WebDeploymentConfiguration;

/**
 * A distributable session management provider.
 * @author Paul Ferraro
 */
public interface DistributableSessionManagementProvider {
    AttachmentKey<DistributableSessionManagementProvider> ATTACHMENT_KEY = AttachmentKey.create(DistributableSessionManagementProvider.class);
    AttachmentKey<AttachmentList<String>> IMMUTABILITY_ATTACHMENT_KEY = AttachmentKey.createList(String.class);

    /**
     * Returns a {@link CapabilityServiceConfigurator} used to configure a service providing a {@link org.wildfly.clustering.web.session.SessionManagerFactory}.
     * @param <S> the HttpSession specification type
     * @param <SC> the ServletContext specification type
     * @param <AL> the HttpSessionAttributeListener specification type
     * @param <MC> the marshalling context type
     * @param <LC> the local context type
     * @param configuration the configuration of the session manager factory
     * @return a service configurator
     */
    <S, SC, AL, MC, LC> CapabilityServiceConfigurator getSessionManagerFactoryServiceConfigurator(SessionManagerFactoryConfiguration<S, SC, AL, MC, LC> configuration);

    /**
     * Returns a {@link CapabilityServiceConfigurator} used to configure a service providing a {@link org.wildfly.clustering.web.routing.RouteLocator}.
     * @param configuration the configuratoin of a deployment
     * @return a service configurator
     */
    CapabilityServiceConfigurator getRouteLocatorServiceConfigurator(WebDeploymentConfiguration configuration);
}
