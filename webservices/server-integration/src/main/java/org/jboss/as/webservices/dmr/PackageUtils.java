/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.dmr;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.CommonConfig;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class PackageUtils {

    private PackageUtils() {
        // forbidden instantiation
    }

    static ServerConfig getServerConfig(final OperationContext context) {
        final ServiceController<?> configService = context.getServiceRegistry(true).getService(WSServices.CONFIG_SERVICE);
        return configService != null ? (ServerConfig)configService.getValue() : null;
    }

    static Collection<? extends CommonConfig> getConfigs(ServerConfig serverConfig, String confType) {
        if (Constants.ENDPOINT_CONFIG.equals(confType)) {
            return serverConfig.getEndpointConfigs();
        } else if (Constants.CLIENT_CONFIG.equals(confType)) {
            return serverConfig.getClientConfigs();
        } else {
            return Collections.emptyList();
        }
    }

}
