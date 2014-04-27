/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.connections.ldap;

import java.net.URI;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * Interface to be implemented by connection managers responsible for returning connections to a LDAP server.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface LdapConnectionManager {

    /**
     * Obtain a connection to LDAP using the configuration defined.
     *
     * @return A {@link DirContext} for access to the LDAP server.
     *
     * @throws NamingException If an error occurs connecting to LDAP.
     */
    DirContext getConnection() throws NamingException;

    /**
     * Verify that the specified bindDn and bindCredential can be used to establish a connection to LDAP.
     *
     * If no {@link NamingException} is thrown then the establishment of the connection was successful.
     *
     * @param bindDn - The bind distinguished name for the connection.
     * @param bindCredential - The bind credential for the connection.
     * @throws NamingException - If there is any error establishing the connection.
     */
    void verifyIdentity(final String bindDn, final String bindCredential) throws NamingException;

    /**
     * Identify which {@link LdapConnectionManager} can establish connections needed for a referral.
     *
     * @param referralUri - The URI of the referral.
     * @return The {@link LdapConnectionManager} that can handle the referral or {@code null} if none is found.
     */
    LdapConnectionManager findForReferral(final URI referralUri);

}
