/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.util;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * WSServices
 *
 * @author alessio.soldano@jboss.com
 * @since 09-Nov-2010
 *
 */
public final class WSServices {

    public static final ServiceName WS_SERVICE = ServiceName.JBOSS.append("ws");
    public static final ServiceName CONFIG_SERVICE = WS_SERVICE.append("config");
    public static final ServiceName CLIENT_CONFIG_SERVICE = WS_SERVICE.append("client-config");
    public static final ServiceName REGISTRY_SERVICE = WS_SERVICE.append("registry");
    public static final ServiceName MODEL_SERVICE = WS_SERVICE.append("model");
    public static final ServiceName ENDPOINT_SERVICE = WS_SERVICE.append("endpoint");
    public static final ServiceName ENDPOINT_PUBLISH_SERVICE = WS_SERVICE.append("endpoint-publish");
    public static final ServiceName PORT_COMPONENT_LINK_SERVICE = WS_SERVICE.append("port-component-link");

    private static ServiceRegistry registry;

    public static void saveContainerRegistry(ServiceRegistry containerRegistry) {
        registry = containerRegistry;
    }

    public static void clearContainerRegistry() {
        registry = null;
    }

    public static ServiceRegistry getContainerRegistry() {
        return registry;
    }

    private WSServices() {
        // forbidden inheritance
    }

}
