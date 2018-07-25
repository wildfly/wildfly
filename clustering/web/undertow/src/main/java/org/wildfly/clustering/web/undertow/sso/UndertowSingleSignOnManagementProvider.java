/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.sso;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.container.HostSingleSignOnManagementConfiguration;
import org.wildfly.clustering.web.container.HostSingleSignOnManagementProvider;
import org.wildfly.clustering.web.sso.DistributableSSOManagementProvider;
import org.wildfly.clustering.web.sso.LegacySSOManagementProviderFactory;
import org.wildfly.extension.undertow.session.SessionManagementProviderFactory;

/**
 * {@link SessionManagementProviderFactory} for Undertow using either the {@link DistributableSSOManagementProvider} for the given host, the default provider, or a legacy provider.
 * @author Paul Ferraro
 */
@MetaInfServices(HostSingleSignOnManagementProvider.class)
public class UndertowSingleSignOnManagementProvider implements HostSingleSignOnManagementProvider {

    private final LegacySSOManagementProviderFactory legacyProviderFactory;

    public UndertowSingleSignOnManagementProvider() {
        Iterator<LegacySSOManagementProviderFactory> factories = ServiceLoader.load(LegacySSOManagementProviderFactory.class, LegacySSOManagementProviderFactory.class.getClassLoader()).iterator();
        if (!factories.hasNext()) {
            throw new ServiceConfigurationError(LegacySSOManagementProviderFactory.class.getName());
        }
        this.legacyProviderFactory = factories.next();
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, HostSingleSignOnManagementConfiguration configuration) {
        return new DistributableSingleSignOnManagerServiceConfigurator(name, configuration, this.legacyProviderFactory);
    }
}
