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

package org.wildfly.iiop.openjdk.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.jboss.iiop.csiv2.SASCurrent;
import org.jboss.security.auth.callback.SecurityAssociationCallback;
import org.jboss.security.auth.spi.AbstractServerLoginModule;

/**
 * Login module that allows a user to authenticate as long as an identity token is present.
 * <p/>
 * Subclasses can override {@link #validateCredential(String, org.wildfly.iiop.openjdk.csiv2.idl.SASCurrent)} to
 * implement identity token validation logic.
 * <p/>
 * WARNING: Installing this class as a login module without subclassing and implementing validation essentially
 * means that the server will trust any incoming CORBA invocation.
 *
 * @author Stuart Douglas
 */
public class TrustedIdentityTokenLoginModule extends AbstractServerLoginModule {

    /**
     * The login identity
     */
    private Principal identity;
    /**
     * The proof of login identity
     */
    private SASCurrent credential;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean login() throws LoginException {
        // See if shared credentials exist
        if (super.login() == true) {
            // Setup our view of the user
            Object username = sharedState.get("javax.security.auth.login.name");
            if (username instanceof Principal)
                identity = (Principal) username;
            else {
                String name = username.toString();
                try {
                    identity = createIdentity(name);
                } catch (Exception e) {
                    LoginException le = new LoginException();
                    le.initCause(e);
                    throw le;
                }
            }
            return true;
        }

        super.loginOk = false;

        if (callbackHandler == null) {
            throw new LoginException();
        }

        SecurityAssociationCallback callback = new SecurityAssociationCallback();
        Callback[] callbacks = {callback};
        final String username;
        try {
            callbackHandler.handle(callbacks);
            username = callback.getPrincipal().getName();
            final Object c = callback.getCredential();
            if (c instanceof SASCurrent) {
                credential = (SASCurrent) c;
            } else {
                return false;
            }

        } catch (IOException e) {
            LoginException le = new LoginException();
            le.initCause(e);
            throw le;
        } catch (UnsupportedCallbackException e) {
            LoginException le = new LoginException();
            le.initCause(e);
            throw le;
        }

        validateCredential(username, credential);

        if (username == null) {
            return false;
        }

        if (identity == null) {
            try {
                identity = createIdentity(username);
            } catch (Exception e) {
                LoginException le = new LoginException();
                le.initCause(e);
                throw le;
            }
        }

        if (getUseFirstPass() == true) {    // Add the principal to the shared state map
            sharedState.put("javax.security.auth.login.name", identity);
            sharedState.put("javax.security.auth.login.password", credential);
        }
        super.loginOk = true;
        return true;
    }


    /**
     * Validates the credential. Unfortunately there is not much we can do here by default.
     * <p/>
     * This method can be overridden to provide some real validation logic
     *
     * @param username   The username
     * @param credential The SASCurrent.
     */
    protected void validateCredential(final String username, final SASCurrent credential) throws LoginException {
        if (credential.get_incoming_principal_name() == null ||
                credential.get_incoming_principal_name().length == 0) {
            throw new LoginException();
        }
    }

    @Override
    protected Principal getIdentity() {
        return identity;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        return new Group[0];
    }

    @Override
    protected Principal getUnauthenticatedIdentity() {
        return unauthenticatedIdentity;
    }

    protected String getUsername() {
        String username = null;
        if (getIdentity() != null)
            username = getIdentity().getName();
        return username;
    }

    protected void safeClose(InputStream fis) {
        try {
            if (fis != null) {
                fis.close();
            }
        } catch (Exception e) {
        }
    }

}
