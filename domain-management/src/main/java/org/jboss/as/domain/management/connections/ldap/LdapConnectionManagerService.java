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
package org.jboss.as.domain.management.connections.ldap;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.util.Properties;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INITIAL_CONTEXT_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SEARCH_CREDENTIAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SEARCH_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

/**
 * The LDAP connection manager to maintain the LDAP connections.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapConnectionManagerService implements Service<LdapConnectionManagerService>, ConnectionManager {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "controller", "management", "connection_manager");

    private volatile ModelNode resolvedConfiguration;

    public LdapConnectionManagerService(final ModelNode resolvedConfiguration) {
        setResolvedConfiguration(resolvedConfiguration);
    }

    void setResolvedConfiguration(final ModelNode resolvedConfiguration) {
        // Validate
        resolvedConfiguration.require(LdapConnectionResourceDefinition.URL.getName());
        resolvedConfiguration.require(LdapConnectionResourceDefinition.SEARCH_DN.getName());
        resolvedConfiguration.require(LdapConnectionResourceDefinition.SEARCH_CREDENTIAL.getName());
        resolvedConfiguration.require(LdapConnectionResourceDefinition.INITIAL_CONTEXT_FACTORY.getName());
        // Store
        this.resolvedConfiguration = resolvedConfiguration;
    }

    /*
    *  Service Lifecycle Methods
    */

    public synchronized void start(StartContext context) throws StartException {
    }

    public synchronized void stop(StopContext context) {
    }

    public synchronized LdapConnectionManagerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /*
     *  Connection Manager Methods
     */

    public Object getConnection() throws Exception {
        final ModelNode config = resolvedConfiguration;
        return getConnection(getFullProperties(config));
    }

    public Object getConnection(String principal, String credential) throws Exception {
        final ModelNode config = resolvedConfiguration;
        Properties connectionProperties = getConnectionOnlyProperties(config);
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

    private Properties getConnectionOnlyProperties(final ModelNode config) {
        final Properties result = new Properties();
        String initialContextFactory = config.require(INITIAL_CONTEXT_FACTORY).asString();
        result.put(Context.INITIAL_CONTEXT_FACTORY,initialContextFactory);
        String url = config.require(URL).asString();
        result.put(Context.PROVIDER_URL,url);
        return result;
    }

    private Properties getFullProperties(final ModelNode config) {
        final Properties result = getConnectionOnlyProperties(config);
        String searchDN = config.require(SEARCH_DN).asString();
        String searchCredential = config.require(SEARCH_CREDENTIAL).asString();
        result.put(Context.SECURITY_PRINCIPAL,searchDN);
        result.put(Context.SECURITY_CREDENTIALS,searchCredential);
        return result;
    }

}
