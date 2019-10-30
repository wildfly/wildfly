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

package org.jboss.as.test.integration.security.loginmodules.common;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;

/**
 * Simple custom login module.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class CustomTestLoginModule implements LoginModule {

    private Subject subject;

    private CallbackHandler callbackHandler;

    private String username;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        String[] s = getUsernameAndPassword();
        username = s[0];
        String password = s[1];
        if (username.equals("anil")) {
            if (password.equals("anil")) { return true; }
        }
        if (username.equals("marcus")) {
            if (password.equals("marcus")) { return true; }
        }
        return false;
    }

    @Override
    public boolean commit() throws LoginException {
        Set<Principal> principals = subject.getPrincipals();
        Group callerPrincipal = new SimpleGroup("CallerPrincipal");
        callerPrincipal.addMember(new SimplePrincipal(username));
        principals.add(callerPrincipal);
        Group roles = new SimpleGroup("Roles");
        if (username.equals("anil")) { roles.addMember(new SimplePrincipal("gooduser")); }
        if (username.equals("marcus")) { roles.addMember(new SimplePrincipal("superuser")); }
        principals.add(roles);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }

    protected String[] getUsernameAndPassword() throws LoginException {
        String[] info = {null, null};
        // prompt for a username and password
        if (callbackHandler == null) {
            throw new LoginException("Error: no CallbackHandler available " + "to collect authentication information");
        }

        NameCallback nc = new NameCallback("User name: ", "guest");
        PasswordCallback pc = new PasswordCallback("Password: ", false);
        Callback[] callbacks = {nc, pc};
        String username = null;
        String password = null;
        try {
            callbackHandler.handle(callbacks);
            username = nc.getName();
            char[] tmpPassword = pc.getPassword();
            if (tmpPassword != null) {
                pc.clearPassword();
                password = new String(tmpPassword);
            }
        } catch (IOException e) {
            LoginException le = new LoginException("Failed to get username/password");
            le.initCause(e);
            throw le;
        } catch (UnsupportedCallbackException e) {
            LoginException le = new LoginException("CallbackHandler does not support: " + e.getCallback());
            le.initCause(e);
            throw le;
        }
        info[0] = username;
        info[1] = password;
        return info;
    }

}
