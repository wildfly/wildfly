/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.connections;

/**
 * A ConnectionManager is responsible for making connections available to the security realms
 * to perform their operations.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface ConnectionManager {

    // TODO - Once both LDAP and DB are implemented create a wrapper exception specific to the ConnectionManager interface.

    /**
     * Obtain a connection based purely on the configuration.
     *
     * @return the ready connected connection.
     */
    Object getConnection() throws Exception;

    /**
     * Obtain a connection based on the configuration but override the
     * principal and credential.
     *
     * This allows for verification that the principal and credential work
     * to connect to the resource.
     *
     * @param principal - The principal to use when connecting.
     * @param credential - The credential to use when connecting.
     * @return the ready connected connection.
     */
    Object getConnection(String principal, String credential) throws Exception;
}
