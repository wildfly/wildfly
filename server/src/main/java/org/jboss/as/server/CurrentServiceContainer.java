/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.server;

import java.security.Permission;

import org.jboss.msc.service.ServiceContainer;

/**
 * Class that provides static access to the current servers ServiceRegistry.
 * <p/>
 * This is not ideal, however there are some places that require access to this
 * where there is no other way of getting it.
 *
 * @author Stuart Douglas
 */
public class CurrentServiceContainer {
    private static final RuntimePermission LOOKUP_CURRENT_SERVICE_CONTAINER = new RuntimePermission("org.jboss.as.server.LOOKUP_CURRENT_SERVICE_CONTAINER");
    private static final RuntimePermission SET_CURRENT_SERVICE_CONTAINER = new RuntimePermission("org.jboss.as.server.SET_CURRENT_SERVICE_CONTAINER");

    private static volatile ServiceContainer serviceContainer;

    public static ServiceContainer getServiceContainer() {
        checkPermission(LOOKUP_CURRENT_SERVICE_CONTAINER);
        return serviceContainer;
    }

    static void setServiceContainer(final ServiceContainer serviceContainer) {
        checkPermission(SET_CURRENT_SERVICE_CONTAINER);
        CurrentServiceContainer.serviceContainer = serviceContainer;
    }

    private static void checkPermission(final Permission permission) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(permission);
        }
    }
}
