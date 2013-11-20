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

import static org.jboss.as.domain.management.DomainManagementLogger.SECURITY_LOGGER;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.SSLContext;

import org.jboss.as.domain.management.SSLIdentity;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * The LDAP connection manager to maintain the LDAP connections.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapConnectionManagerService implements Service<LdapConnectionManagerService>, ConnectionManager {

    private static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "controller", "management", "connection_manager");

    private final InjectedValue<SSLIdentity> sslIdentity = new InjectedValue<SSLIdentity>();

    private volatile Config configuration;
    private volatile Hashtable<String, String> properties = new Hashtable<String, String>();

    public LdapConnectionManagerService() {
    }

    Config setConfiguration(final String initialContextFactory, final String url, final String searchDn, final String searchCredential) {
        Config configuration = new Config();
        configuration.initialContextFactory = initialContextFactory;
        configuration.url = url;
        configuration.searchDn = searchDn;
        configuration.searchCredential = searchCredential;

        try {
            return this.configuration;
        } finally {
            this.configuration = configuration;
        }
    }

    void setConfiguration(final Config configuration) {
        this.configuration = configuration;
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

    public InjectedValue<SSLIdentity> getSSLIdentityInjector() {
        return sslIdentity;
    }

    /*
     * Property Manipulation Methods.
     */

    synchronized void setProperty(final String name, final String value) {
        Hashtable<String, String> properties = new Hashtable<String, String>(this.properties);
        properties.put(name, value);

        this.properties = properties;
    }

    synchronized void removeProperty(final String name) {
        Hashtable<String, String> properties = new Hashtable<String, String>(this.properties);
        properties.remove(name);

        this.properties = properties;
    }

    void setPropertyImmediate(final String name, final String value) {
        properties.put(name, value);
    }

    /*
     *  Connection Manager Methods
     */

    public Object getConnection() throws Exception {
        return getConnection(getFullProperties(configuration), getSSLContext(false));
    }

    public Object getConnection(String principal, String credential) throws Exception {
        Hashtable<String, String> connectionProperties = getConnectionOnlyProperties(configuration);
        connectionProperties.put(Context.SECURITY_PRINCIPAL, principal);
        connectionProperties.put(Context.SECURITY_CREDENTIALS, credential);

        // Use a trust only SSLContext as we do not want to authenticate using a pre-defined key in a KeyStore.
        return getConnection(connectionProperties, getSSLContext(true));
    }

    private Object getConnection(final Hashtable<String, String> properties, final SSLContext sslContext) throws Exception {
        ClassLoader old = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            if (sslContext != null) {
                ThreadLocalSSLSocketFactory.setSSLSocketFactory(sslContext.getSocketFactory());
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(ThreadLocalSSLSocketFactory.class);
                properties.put("java.naming.ldap.factory.socket", ThreadLocalSSLSocketFactory.class.getName());
            }
            if (SECURITY_LOGGER.isTraceEnabled()) {
                Hashtable<String, String> logProperties;
                if (properties.containsKey(Context.SECURITY_CREDENTIALS)) {
                    logProperties = new Hashtable<String, String>(properties);
                    logProperties.put(Context.SECURITY_CREDENTIALS, "***");
                } else {
                    logProperties = properties;
                }
                SECURITY_LOGGER.tracef("Connecting to LDAP with properties (%s)", logProperties.toString());
            }

            return new InitialDirContext(properties);
        } finally {
            if (sslContext != null) {
                ThreadLocalSSLSocketFactory.removeSSLSocketFactory();
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
        }
    }

    private SSLContext getSSLContext(final boolean trustOnly) {
        SSLIdentity sslIdentityValue = sslIdentity.getOptionalValue();
        if (sslIdentityValue != null) {
            return trustOnly ? sslIdentityValue.getTrustOnlyContext() : sslIdentityValue.getFullContext();
        }

        return null;
    }

    private Hashtable<String, String> getConnectionOnlyProperties(final Config configuration) {
        final Hashtable<String, String> result = new Hashtable<String, String>(properties);
        result.put(Context.INITIAL_CONTEXT_FACTORY, configuration.initialContextFactory);
        result.put(Context.PROVIDER_URL, configuration.url);
        return result;
    }

    private Hashtable<String, String> getFullProperties(final Config configuration) {
        final Hashtable<String, String> result = getConnectionOnlyProperties(configuration);
        // These are no longer mandatory as the SSL identity of the server
        // could be used instead.
        if (configuration.searchDn != null) {
            result.put(Context.SECURITY_PRINCIPAL, configuration.searchDn);
        }
        if (configuration.searchCredential != null) {
            result.put(Context.SECURITY_CREDENTIALS, configuration.searchCredential);
        }

        return result;
    }

    public static final class ServiceUtil {

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String connectionName) {
            return BASE_SERVICE_NAME.append(connectionName);
        }

        public static ServiceBuilder<?> addDependency(ServiceBuilder<?> sb, Injector<ConnectionManager> injector,
                String connectionName, boolean optional) {
            ServiceBuilder.DependencyType type = optional ? ServiceBuilder.DependencyType.OPTIONAL : ServiceBuilder.DependencyType.REQUIRED;
            sb.addDependency(type, createServiceName(connectionName), ConnectionManager.class, injector);

            return sb;
        }
    }

    static class Config {
        private String initialContextFactory;
        private String url;
        private String searchDn;
        private String searchCredential;
    }

}
