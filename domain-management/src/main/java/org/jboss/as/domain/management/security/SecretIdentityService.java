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

import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;

import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.util.Base64;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import java.io.IOException;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;


/**
 * A simple identity service for an identity represented by a single secret or password.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecretIdentityService implements Service<CallbackHandlerFactory> {

    public static final String SERVICE_SUFFIX = "secret";

    private final String password;
    private final boolean base64;

    private volatile CallbackHandlerFactory factory;

    public SecretIdentityService(final String password, boolean base64) {
        this.password = password;
        this.base64 = base64;
    }

    public void start(StartContext startContext) throws StartException {
        final char[] thePassword;
        if (base64) {
            byte[] value = Base64.decode(password);
            String tempPassword = new String(value);
            String trimmedPassword = tempPassword.trim();
            if (tempPassword.equals(trimmedPassword) == false) {
                ROOT_LOGGER.whitespaceTrimmed();
            }

            thePassword = trimmedPassword.toCharArray();
        } else {
            thePassword = password.toCharArray();
        }

        factory = new CallbackHandlerFactory() {
            public CallbackHandler getCallbackHandler(String username) {
                return new SecretCallbackHandler(username, thePassword);
            }
        };
    }

    public void stop(StopContext stopContext) {
        factory = null;
    }

    public CallbackHandlerFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return factory;
    }

    private class SecretCallbackHandler implements CallbackHandler {

        private final String userName;
        private final char[] password;

        SecretCallbackHandler(final String userName, final char[] password) {
            this.userName = userName;
            this.password = password;
        }


        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback current : callbacks) {
                if (current instanceof RealmCallback) {
                    RealmCallback rcb = (RealmCallback) current;
                    String defaultText = rcb.getDefaultText();
                    rcb.setText(defaultText); // For now just use the realm suggested.
                } else if (current instanceof RealmChoiceCallback) {
                    throw MESSAGES.realmNotSupported(current);
                } else if (current instanceof NameCallback) {
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
    }
}
