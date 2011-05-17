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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.KEYSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERS;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The service representing the security realm, this service will be injected into any management interfaces
 * requiring any of the capabilities provided by the realm.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityRealmService implements Service<SecurityRealmService>, SecurityRealm {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "controller", "management", "security_realm");
    private static final Logger log = Logger.getLogger("org.jboss.as");

    private final InjectedValue<ConnectionManager> connectionManagerValue = new InjectedValue<ConnectionManager>();
    private final InjectedValue<String> relativeTo = new InjectedValue<String>();
    private final InjectedValue<SSLIdentityService> sslIdentity = new InjectedValue<SSLIdentityService>();

    private final String name;

    private ModelNode authentication;
    private DomainCallbackHandler callbackHandler;

    private SSLContext sslContext;

    public SecurityRealmService(String name, ModelNode authentication) {
        this.name = name;
        this.authentication = authentication;
    }

    public void start(StartContext context) throws StartException {
        log.infof("Starting '%s' Security Realm Service", name);
        if (authentication != null && authentication.has(USERS)) {
            callbackHandler = new UserDomainCallbackHandler(name, authentication.require(USERS));
        } else if (authentication != null && authentication.has(LDAP)) {
            callbackHandler = new UserLdapCallbackHandler(connectionManagerValue.getValue(), authentication.require(LDAP));
        } else {
            callbackHandler = new DomainCallbackHandler() {

                public Class[] getSupportedCallbacks() {
                    return new Class[0];
                }

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    // TODO - Should be a real error with code.
                    throw new IllegalStateException("No authentication mechanism defined in security realm.");
                }
            };
        }
    }

    public void stop(StopContext context) {
        log.infof("Stopping '%s' Security Realm Service", name);
    }

    public SecurityRealmService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public String getName() {
        return name;
    }

    public InjectedValue<ConnectionManager> getConnectionManagerInjector() {
        return connectionManagerValue;
    }


    public InjectedValue<SSLIdentityService> getSSLIdentityInjector() {
        return sslIdentity;
    }


    /**
     * Used to obtain the callback handler for the configured 'authorizations'.
     *
     * @return The CallbackHandler to be used for verifying the identity of the caller.
     */
    public DomainCallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public SSLContext getSSLContext() {
        SSLIdentityService service = sslIdentity.getOptionalValue();
        if (sslIdentity != null) {
            return service.getSSLContext();
        }

        return null;
    }

}
