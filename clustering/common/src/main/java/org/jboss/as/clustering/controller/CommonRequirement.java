/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import javax.management.MBeanServer;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.network.SocketBindingManager;
import org.wildfly.clustering.service.Requirement;

/**
 * Enumerates common requirements for clustering resources.
 * @author Paul Ferraro
 */
public enum CommonRequirement implements Requirement, ServiceNameFactoryProvider {
    ELYTRON("org.wildfly.security.elytron", Void.class),
    MBEAN_SERVER("org.wildfly.management.jmx", MBeanServer.class),
    NAMING_STORE(NamingService.CAPABILITY_NAME, NamingStore.class),
    PATH_MANAGER("org.wildfly.management.path-manager", PathManager.class),
    SOCKET_BINDING_MANAGER("org.wildfly.management.socket-binding-manager", SocketBindingManager.class),
    ;
    private final String name;
    private final Class<?> type;
    private final ServiceNameFactory factory = new RequirementServiceNameFactory(this);

    CommonRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public ServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
