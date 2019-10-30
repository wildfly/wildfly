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

package org.jboss.as.security.remoting;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.jboss.as.core.security.RealmUser;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.callback.ObjectCallback;
import org.jboss.security.auth.spi.AbstractServerLoginModule;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * A simple LoginModule to take the UserPrincipal from the inbound Remoting connection and to use it as an already authenticated
 * user.
 *
 * Subsequent login modules can be chained after this module to load role information.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemotingLoginModule extends AbstractServerLoginModule {

    /**
     * If a {@link javax.security.cert.X509Certificate} is available from the client as a result of a {@link SSLSession} being established should
     * this be used for the credential.
     *
     * Default = false.
     */
    private static final String USE_CLIENT_CERT_OPTION = "useClientCert";

    /**
     * If a {@link java.security.cert.X509Certificate} is available from the client as a result of a {@link SSLSession} being established should
     * this be used for the credential.
     *
     * Default = false.
     */
    private static final String USE_NEW_CLIENT_CERT_OPTION = "useNewClientCert";

    private static final String[] ALL_OPTIONS = new String[] { USE_CLIENT_CERT_OPTION, USE_NEW_CLIENT_CERT_OPTION };

    private boolean useClientCert = false;
    private boolean useNewClientCert = false;
    private Principal identity;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        addValidOptions(ALL_OPTIONS);
        super.initialize(subject, callbackHandler, sharedState, options);

        if (options.containsKey(USE_CLIENT_CERT_OPTION)) {
            useClientCert = Boolean.parseBoolean(options.get(USE_CLIENT_CERT_OPTION).toString());
        }
        if (options.containsKey(USE_NEW_CLIENT_CERT_OPTION)) {
            useNewClientCert = Boolean.parseBoolean(options.get(USE_NEW_CLIENT_CERT_OPTION).toString());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean login() throws LoginException {
        if (super.login() == true) {
            log.debug("super.login()==true");
            return true;
        }

        Object credential = getCredential();
        if (credential instanceof RemotingConnectionCredential) {
            final RemotingConnectionCredential remotingConnectionCredential = (RemotingConnectionCredential) credential;
            SecurityIdentity localIdentity = remotingConnectionCredential.getSecurityIdentity();
            identity = new RealmUser(localIdentity.getPrincipal().getName());
            if (getUseFirstPass()) {
                String userName = identity.getName();
                log.debugf("Storing username '%s'", userName);
                // Add the username to the shared state map
                sharedState.put("javax.security.auth.login.name", identity);

                if (useNewClientCert) {
                    SSLSession session = remotingConnectionCredential.getSSLSession();
                    if (session != null) {
                        try {
                            credential = session.getPeerCertificates()[0];
                            log.debug("Using new certificate as credential.");
                        } catch (SSLPeerUnverifiedException e) {
                            log.debugf("No peer certificate available for '%s'", userName);
                        }
                    }
                } else if (useClientCert) {
                    SSLSession session = remotingConnectionCredential.getSSLSession();
                    if (session != null) {
                        try {
                            credential = session.getPeerCertificateChain()[0];
                            log.debug("Using certificate as credential.");
                        } catch (SSLPeerUnverifiedException e) {
                            log.debugf("No peer certificate available for '%s'", userName);
                        }
                    }
                }
                sharedState.put("javax.security.auth.login.password", credential);
            }
            loginOk = true;
            return true;
        }

        // We return false to allow the next module to attempt authentication, maybe a
        // username and password has been supplied to a web auth.
        return false;
    }

    protected Object getCredential() throws LoginException {
        NameCallback nc = new NameCallback("Alias: ");
        ObjectCallback oc = new ObjectCallback("Credential: ");
        Callback[] callbacks = { nc, oc };

        try {
            callbackHandler.handle(callbacks);

            return oc.getCredential();
        } catch (IOException ioe) {
            LoginException le = new LoginException();
            le.initCause(ioe);
            throw le;
        } catch (UnsupportedCallbackException uce) {
            LoginException le = new LoginException();
            le.initCause(uce);
            throw le;
        }
    }

    @Override
    protected Principal getIdentity() {
        return identity;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        Group roles = new SimpleGroup("Roles");
        Group callerPrincipal = new SimpleGroup("CallerPrincipal");
        Group[] groups = { roles, callerPrincipal };
        callerPrincipal.addMember(getIdentity());
        return groups;
    }

}
