/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.config;

import org.jboss.as.webservices.WSServices;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.ioc.IoCContainerProxy;
import org.jboss.wsf.spi.ioc.IoCContainerProxyFactory;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.management.ServerConfigFactory;

/**
 * Retrieves webservices stack specific config.
 *
 * @author <a href="mailto:hbraun@redhat.com">Heiko Braun</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ServerConfigFactoryImpl extends ServerConfigFactory {
    /**
     * Constructor.
     */
    public ServerConfigFactoryImpl() {
        super();
    }

    /**
     * Returns config registered in MC kernel.
     *
     * @return config
     */
    public ServerConfig getServerConfig() {
        final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
        final IoCContainerProxyFactory iocContainerFactory = spiProvider.getSPI(IoCContainerProxyFactory.class);
        final IoCContainerProxy iocContainer = iocContainerFactory.getContainer();

        return iocContainer.getBean(WSServices.CONFIG_SERVICE.getCanonicalName(), ServerConfig.class); //TODO!! review IoCContainer spi to avoid conversion to/from String/Service
    }
}
