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

import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;

import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class PackageUtils {

    private PackageUtils() {
        // forbidden instantiation
    }

    static ServiceName getEndpointConfigServiceName(String configName) {
        return WSServices.ENDPOINT_CONFIG_SERVICE.append(configName);
    }

    static ServiceName getClientConfigServiceName(String configName) {
        return WSServices.CLIENT_CONFIG_SERVICE.append(configName);
    }

    static ServiceName getConfigServiceName(final String configType, final String configName) {
        return (ENDPOINT_CONFIG.equals(configType) ? getEndpointConfigServiceName(configName) : getClientConfigServiceName(configName));
    }

    static ServiceName getHandlerChainServiceName(final String configType, final String configName, final String handlerChainId) {
        return getHandlerChainServiceName(getConfigServiceName(configType, configName), handlerChainId);
    }

    static ServiceName getHandlerChainServiceName(final ServiceName configServiceName, final String handlerChainId) {
        return configServiceName.append(HANDLER_CHAIN).append(handlerChainId);
    }

    static ServiceName getHandlerServiceName(final ServiceName handlerChainServiceName, final String handlerName) {
        return handlerChainServiceName.append(HANDLER).append(handlerName);
    }

    static ServiceName getPropertyServiceName(final ServiceName configServiceName, final String propertyName) {
        return configServiceName.append(PROPERTY).append(propertyName);
    }

}