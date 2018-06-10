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

import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.SimpleCapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.sso.SSOManagerFactoryServiceConfiguratorProvider;
import org.wildfly.extension.undertow.security.sso.DistributableHostSingleSignOnManagerServiceConfiguratorProvider;

import io.undertow.security.impl.InMemorySingleSignOnManager;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DistributableHostSingleSignOnManagerServiceConfiguratorProvider.class)
public class DistributableSingleSignOnManagerServiceConfiguratorProvider implements DistributableHostSingleSignOnManagerServiceConfiguratorProvider {

    private static final SSOManagerFactoryServiceConfiguratorProvider PROVIDER = loadProvider();

    private static SSOManagerFactoryServiceConfiguratorProvider loadProvider() {
        for (SSOManagerFactoryServiceConfiguratorProvider provider : ServiceLoader.load(SSOManagerFactoryServiceConfiguratorProvider.class, SSOManagerFactoryServiceConfiguratorProvider.class.getClassLoader())) {
            return provider;
        }
        return null;
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, String serverName, String hostName) {
        return (PROVIDER != null) ? new DistributableSingleSignOnManagerServiceConfigurator(name, serverName, hostName, PROVIDER) : new SimpleCapabilityServiceConfigurator<>(name, new InMemorySingleSignOnManager());
    }
}
