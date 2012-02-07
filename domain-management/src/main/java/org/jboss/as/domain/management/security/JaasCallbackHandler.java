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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.sasl.callback.VerifyPasswordCallback;

/**
 * A CallbackHandler verifying users usernames and passwords by using a JAAS LoginContext.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JaasCallbackHandler implements Service<DomainCallbackHandler>, DomainCallbackHandler {

    public static final String SERVICE_SUFFIX = "jaas";

    private static final Class[] CALLBACKS = { AuthorizeCallback.class, RealmCallback.class, NameCallback.class,
            VerifyPasswordCallback.class, SubjectCallback.class };

    private final String name;

    public JaasCallbackHandler(final String name) {
        this.name = name;
    }

    /*
     * DomainCallbackHandler Methods
     */

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (callbacks.length == 1 && callbacks[0] instanceof AuthorizeCallback) {
            AuthorizeCallback acb = (AuthorizeCallback) callbacks[0];
            String authenticationId = acb.getAuthenticationID();
            String authorizationId = acb.getAuthorizationID();

            acb.setAuthorized(authenticationId.equals(authorizationId));

            return;
        }

        NameCallback nameCallBack = null;
        VerifyPasswordCallback verifyPasswordCallback = null;
        SubjectCallback subjectCallback = null;

        for (Callback current : callbacks) {
            if (current instanceof NameCallback) {
                nameCallBack = (NameCallback) current;
            } else if (current instanceof RealmCallback) {
            } else if (current instanceof VerifyPasswordCallback) {
                verifyPasswordCallback = (VerifyPasswordCallback) current;
            } else if (current instanceof SubjectCallback) {
                subjectCallback = (SubjectCallback) current;
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }

        if (nameCallBack == null) {
            throw MESSAGES.noUsername();
        }
        final String userName = nameCallBack.getDefaultName();
        if (userName == null || userName.length() == 0) {
            throw MESSAGES.noUsername();
        }
        if (verifyPasswordCallback == null || verifyPasswordCallback.getPassword() == null) {
            throw MESSAGES.noPassword();
        }
        final char[] password = verifyPasswordCallback.getPassword().toCharArray();

        try {
            Subject subject = subjectCallback != null && subjectCallback.getSubject() != null ? subjectCallback.getSubject() : new Subject();
            LoginContext ctx = new LoginContext(name, subject, new CallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback current : callbacks) {
                        if (current instanceof NameCallback) {
                            NameCallback ncb = (NameCallback) current;
                            ncb.setName(userName);
                        } else if (current instanceof PasswordCallback) {
                            PasswordCallback pcb = (PasswordCallback) current;
                            pcb.setPassword(password);
                        } else {
                            throw new UnsupportedCallbackException(current);
                        }
                    }
                }
            });
            ctx.login();
            verifyPasswordCallback.setVerified(true);
            subject.getPrivateCredentials().add(new PasswordCredential(userName, password));
            if (subjectCallback != null) {
                // Only want to deliberately pass it back if authentication completed.
                subjectCallback.setSubject(subject);
            }
        } catch (LoginException e) {
            verifyPasswordCallback.setVerified(false);
        }
    }

    public Class[] getSupportedCallbacks() {
        return CALLBACKS;
    }

    public boolean isReady() {
        return true;
    }

    /*
     * Service Methods
     */

    public void start(final StartContext context) throws StartException {
    }

    public void stop(final StopContext context) {
    }

    public DomainCallbackHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

}
