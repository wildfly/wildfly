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

package org.wildfly.clustering.web.undertow.session;

import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.SimpleCapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.session.SessionManagerFactoryServiceConfiguratorProvider;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.session.DistributableSessionManagerConfiguration;

import io.undertow.servlet.core.InMemorySessionManagerFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryServiceConfiguratorProvider.class)
public class DistributableSessionManagerFactoryServiceConfiguratorProvider implements org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryServiceConfiguratorProvider {

    private static final SessionManagerFactoryServiceConfiguratorProvider PROVIDER = loadProvider();

    private static SessionManagerFactoryServiceConfiguratorProvider loadProvider() {
        for (SessionManagerFactoryServiceConfiguratorProvider provider : ServiceLoader.load(SessionManagerFactoryServiceConfiguratorProvider.class, SessionManagerFactoryServiceConfiguratorProvider.class.getClassLoader())) {
            return provider;
        }
        return null;
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, DistributableSessionManagerConfiguration configuration) {
        if (PROVIDER == null) {
            UndertowLogger.ROOT_LOGGER.clusteringNotSupported();
            return new SimpleCapabilityServiceConfigurator<>(name, new InMemorySessionManagerFactory(configuration.getMaxActiveSessions()));
        }
        return new DistributableSessionManagerFactoryServiceConfigurator(name, configuration, PROVIDER);
    }
}
