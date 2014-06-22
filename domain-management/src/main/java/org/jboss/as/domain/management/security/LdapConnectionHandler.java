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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.jboss.as.domain.management.connections.ldap.LdapConnectionManager;

/**
 * Utility for managing LDAP connections.
 *
 * An instance of this utility is created at the start of authentication processing and is used both for authentication and
 * group loading, connections returned by this connection without specifying a {@code bindDn} and {@code bindCredential} will be
 * cached until {@code close()} is called.
 *
 * This class is not thread safe and must not be shared by multiple threads concurrently.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class LdapConnectionHandler implements LdapConnectionManager, Closeable {

    /**
     * The base LdapConnectionManager for obtaining connections.
     */
    private final LdapConnectionManager ldapConnectionManager;

    private final LdapConnectionHandler parent;
    private Map<URI, LdapConnectionHandler> cachedLdapConnectionHandlers = null;
    private DirContext cachedDirContext;

    private LdapConnectionHandler(final LdapConnectionManager ldapConnectionManager) {
        this.ldapConnectionManager = ldapConnectionManager;
        parent = null;
    }

    private LdapConnectionHandler(final LdapConnectionHandler parent, final LdapConnectionManager ldapConnectionManager) {
        this.parent = parent;
        this.ldapConnectionManager = ldapConnectionManager;
    }

    static LdapConnectionHandler newInstance(final LdapConnectionManager ldapConnectionManager) {
        return new LdapConnectionHandler(ldapConnectionManager);
    }

    @Override
    public DirContext getConnection() throws NamingException {
        if (cachedDirContext != null) {
            return cachedDirContext;
        }

        return (cachedDirContext = ldapConnectionManager.getConnection());
    }

    @Override
    public void verifyIdentity(String bindDn, String bindCredential) throws NamingException {
        ldapConnectionManager.verifyIdentity(bindDn, bindCredential);
    }

    @Override
    public LdapConnectionHandler findForReferral(URI referralUri) {
        if (cachedLdapConnectionHandlers != null && cachedLdapConnectionHandlers.containsKey(referralUri)) {
            return cachedLdapConnectionHandlers.get(referralUri);
        }

        LdapConnectionManager nextConnectionManager = ldapConnectionManager.findForReferral(referralUri);
        if (nextConnectionManager == null) {
            return null;
        }

        final LdapConnectionHandler nextLdapConnectionHandler;
        if (nextConnectionManager == ldapConnectionManager) {
            /*
             * This would cover a referral to a different DN on the same server.
             */
            nextLdapConnectionHandler = this;
        } else {
            nextLdapConnectionHandler = new LdapConnectionHandler(this, nextConnectionManager);
        }
        cache(referralUri, nextLdapConnectionHandler);
        return nextLdapConnectionHandler;
    }

    @Override
    public void close() throws IOException {
        safeClose(cachedDirContext);
        if (cachedLdapConnectionHandlers != null) {
            for (LdapConnectionHandler current : cachedLdapConnectionHandlers.values()) {
                if (current != null) {
                    current.close();
                }
            }
            cachedLdapConnectionHandlers.clear();
        }
    }

    private void safeClose(DirContext d) {
        if (d != null) {
            try {
                d.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void cache(final URI uri, final LdapConnectionHandler handler) {
        if (parent != null) {
            parent.cache(uri, handler);
        } else {
            if (cachedLdapConnectionHandlers == null) {
                cachedLdapConnectionHandlers = new HashMap<URI, LdapConnectionHandler>(1);
            }
            cachedLdapConnectionHandlers.put(uri, handler);
        }

    }

}
