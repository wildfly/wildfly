/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.integration.elytron.realm;

import org.wildfly.security.auth.principal.NamePrincipal;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link LoginModule} implementation used in the JAAS security realm via a custom realm tests.
 * accepts only credentials user1 with password1 and this user has role Role1
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class TestLoginModule implements LoginModule {

    private final Map<String, char[]> usersMap = new HashMap<>();
    private Subject subject;
    private CallbackHandler handler;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.handler = callbackHandler;
        this.usersMap.put("user1", "password1".toCharArray());
        this.usersMap.put("user2", "password2".toCharArray());
    }

    @Override
    public boolean login() throws LoginException {
        // obtain the incoming username and password from the callback handler
        NameCallback nameCallback = new NameCallback("Username");
        PasswordCallback passwordCallback = new PasswordCallback("Password", false);
        Callback[] callbacks = new Callback[]{nameCallback, passwordCallback};
        try {
            this.handler.handle(callbacks);
        } catch (UnsupportedCallbackException | IOException e) {
            throw new LoginException("Error handling callback: " + e.getMessage());
        }

        final String username = nameCallback.getName();
        final char[] password = passwordCallback.getPassword();

        char[] storedPassword = this.usersMap.get(username);
        boolean success = password != null && username != null && Arrays.equals(storedPassword, password);
        if (success) {
            this.subject.getPrincipals().add(new NamePrincipal(username));
            if (username.equals("user1")) {
                this.subject.getPrincipals().add(new Roles("Role1"));
            } else if (username.equals("user2")) {
                this.subject.getPrincipals().add(new Roles("Role2"));
            }
        }
        return success;
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        this.subject.getPrincipals().clear();
        return true;
    }

    private static class Roles implements Principal {

        private final String name;

        Roles(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
