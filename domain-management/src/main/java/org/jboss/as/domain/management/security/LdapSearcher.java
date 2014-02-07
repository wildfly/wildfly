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
package org.jboss.as.domain.management.security;

import java.io.IOException;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * Interface for a LDAP searcher, this could be a search for users or a search for groups.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
interface LdapSearcher<R, K> {

    /**
     * Perform a search against LDAP.
     *
     * @param context - The {@link DirContext} to use to access LDAP.
     * @param key - The base key to use as the search.
     * @return The search result.
     * @throws IOException - If an error occurs communicating with LDAP.
     * @throws NamingException - If an error is encountered searching LDAP.
     */
    R search(final DirContext context, final K key) throws IOException, NamingException;

}
