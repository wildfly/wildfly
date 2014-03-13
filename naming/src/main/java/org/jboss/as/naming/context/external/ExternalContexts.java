/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming.context.external;

import org.jboss.msc.service.ServiceName;

/**
 * The external contexts which exist in Wildfly's Naming subsystem.
 * @author Eduardo Martins
 */
public interface ExternalContexts {

    /**
     * Adds an external context.
     * @param serviceName the external context's service name
     * @throws IllegalArgumentException if there is already an external context with such service name, or if there is already an external context which is a child or parent of the one to add
     */
    void addExternalContext(ServiceName serviceName) throws IllegalArgumentException;

    /**
     * Removes an external context.
     * @param serviceName the external context's service name
     * @return true if an external context with the specified jndi name was found and removed.
     */
    boolean removeExternalContext(ServiceName serviceName);

    /**
     * Retrieves the external context that is a parent of the specified child service name.
     * @param childServiceName a external context's child service name
     * @return null if there is currently no external context, which is a parent of the specified service name.
     */
    ServiceName getParentExternalContext(ServiceName childServiceName);
}
