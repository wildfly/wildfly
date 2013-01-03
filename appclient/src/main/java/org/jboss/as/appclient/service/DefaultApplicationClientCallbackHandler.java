/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.appclient.service;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.identity.Identity;
import org.jboss.security.identity.extensions.CredentialIdentity;

import static java.security.AccessController.doPrivileged;

/**
 * The default callback handler used by the
 *
 * @author Stuart Douglas
 */
public class DefaultApplicationClientCallbackHandler implements CallbackHandler {

    public static final String DOLLAR_LOCAL = "$local";

    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        final SecurityContext context = doPrivileged(securityContext());

        for (final Callback current : callbacks) {
            if (current instanceof NameCallback) {
                final NameCallback ncb = (NameCallback) current;
                if (context != null) {
                    final Set<Identity> identities = context.getSubjectInfo().getIdentities();
                    if (identities.isEmpty()) {
                        ncb.setName(DOLLAR_LOCAL);
                    } else {
                        final Identity identity = identities.iterator().next();
                        ncb.setName(identity.getName());
                    }
                } else {
                    ncb.setName(DOLLAR_LOCAL);
                }
            } else if (current instanceof PasswordCallback) {
                if (context != null) {
                    final PasswordCallback pcb = (PasswordCallback) current;
                    final Set<Identity> identities = context.getSubjectInfo().getIdentities();
                    if (identities.isEmpty()) {
                        throw new UnsupportedCallbackException(current);
                    } else {
                        final Identity identity = identities.iterator().next();
                        if (identity instanceof CredentialIdentity) {
                            pcb.setPassword((char[]) ((CredentialIdentity) identity).getCredential());
                        } else {
                            throw new UnsupportedCallbackException(current);
                        }
                    }
                }
            } else if (current instanceof RealmCallback) {
                final RealmCallback realmCallback = (RealmCallback) current;
                if (realmCallback.getText() == null) {
                    realmCallback.setText(realmCallback.getDefaultText());
                }
            }
        }
    }


    private static PrivilegedAction<SecurityContext> securityContext() {
        return new PrivilegedAction<SecurityContext>() {
            public SecurityContext run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        };
    }
}
