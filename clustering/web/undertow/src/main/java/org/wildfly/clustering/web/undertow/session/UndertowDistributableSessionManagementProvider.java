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

package org.wildfly.clustering.web.undertow.session;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.container.WebDeploymentConfiguration;
import org.wildfly.clustering.web.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.undertow.routing.DistributableSessionIdentifierCodecServiceConfigurator;

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
    public CapabilityServiceConfigurator getSessionIdentifierCodecServiceConfigurator(ServiceName name, WebDeploymentConfiguration configuration) {
        return new DistributableSessionIdentifierCodecServiceConfigurator(name, new WebDeploymentConfigurationAdapter(configuration), this.provider);
    }

    @Override
    public CapabilityServiceConfigurator getSessionManagerFactoryServiceConfigurator(ServiceName name, SessionManagerFactoryConfiguration configuration) {
        return new DistributableSessionManagerFactoryServiceConfigurator(name, configuration, this.provider, this.immutability);
    }
}
