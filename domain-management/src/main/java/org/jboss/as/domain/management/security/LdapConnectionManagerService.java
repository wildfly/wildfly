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
package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INITIAL_CONTEXT_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SEARCH_CREDENTIAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SEARCH_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.util.Properties;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The LDAP connection manager to maintain the LDAP connections.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapConnectionManagerService implements Service<LdapConnectionManagerService>, ConnectionManager {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "controller", "management", "connection_manager");

    private static final String DEFAULT_INITIAL_CONTEXT = "com.sun.jndi.ldap.LdapCtxFactory";

    /* Contains connection information only with no principal or credentials. */
    private Properties connectionOnlyProperties;
    /* As connectionOnlyProperties but with added principal and credential. */
    private Properties fullProperties;

    private final ModelNode ldapConnection;

    public LdapConnectionManagerService(final ModelNode ldapConnection) {
        this.ldapConnection = ldapConnection;
    }

    /*
    *  Service Lifecycle Methods
    */

    public void start(StartContext context) throws StartException {
        connectionOnlyProperties = new Properties();

        connectionOnlyProperties.put(Context.SECURITY_AUTHENTICATION,"simple");

        String initialContextFactory = DEFAULT_INITIAL_CONTEXT;
        if (ldapConnection.has(INITIAL_CONTEXT_FACTORY)) {
            initialContextFactory = ldapConnection.require(INITIAL_CONTEXT_FACTORY).asString();
        }
        connectionOnlyProperties.put(Context.INITIAL_CONTEXT_FACTORY,initialContextFactory);

        String url = ldapConnection.require(URL).asString();
        connectionOnlyProperties.put(Context.PROVIDER_URL,url);

        fullProperties = (Properties)connectionOnlyProperties.clone();

        String searchDN = ldapConnection.require(SEARCH_DN).asString();
        String searchCredential = ldapConnection.require(SEARCH_CREDENTIAL).asString();

        fullProperties.put(Context.SECURITY_PRINCIPAL,searchDN);
        fullProperties.put(Context.SECURITY_CREDENTIALS,searchCredential);
    }

    public void stop(StopContext context) {
        connectionOnlyProperties = null;
        fullProperties = null;
    }

    public LdapConnectionManagerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /*
     *  Connection Manager Methods
     */

    public Object getConnection() throws Exception {
        return getConnection(fullProperties);
    }

    public Object getConnection(String principal, String credential) throws Exception {
        Properties connectionProperties = (Properties) connectionOnlyProperties.clone();
        connectionProperties.put(Context.SECURITY_PRINCIPAL, principal);
        connectionProperties.put(Context.SECURITY_CREDENTIALS, credential);

        return getConnection(connectionProperties);
    }

    // TODO - Workaround to clear ContextClassLoader to allow access to System ClassLoader
    private Object getConnection(Properties properties) throws Exception {
        ClassLoader original = null;
        try {
            original = Thread.currentThread().getContextClassLoader();
            if (original != null) {
                Thread.currentThread().setContextClassLoader(null);
            }
            return new InitialDirContext(properties);
        } finally {
            if (original != null) {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }

}
