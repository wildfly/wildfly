/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
import java.util.Properties;
import java.util.Set;
import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 *         <p/>
 *         This is a helper login module to be used together with RunAsLoginModule during RunAs loginModuleTests.
 *         It access a secured EJB during authentication. The security role needed to access this EJB must be provided
 *         by RunAsLoginModule.
 */
public class CustomEjbAccessingLoginModule implements LoginModule {


    @EJB
    private SimpleSecuredEJB simpleSecuredEJB;

    private Subject subject;
    private CallbackHandler callbackHandler;
    private String username;
    private String password;

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {

        try {
            Properties env = new Properties();
            env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            InitialContext context = new InitialContext(env);
            simpleSecuredEJB = (SimpleSecuredEJB) context.lookup("java:global/RunAsLoginModule/SimpleSecuredEJBImpl");
        } catch (NamingException ex) {
            throw new RuntimeException(ex);
        }

        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    public boolean login() throws LoginException {
        getUsernameAndPassword();
        //check that the login module has "RunAsLoginModuleRole" role
        simpleSecuredEJB.accessRunAsLoginModuleRole();

        if (username.equals("anil")) {
            if (password.equals("anil")) { return true; }
        }
        if (username.equals("marcus")) {
            if (password.equals("marcus")) { return true; }
        }

        return false;
    }


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


    public boolean abort() throws LoginException {
        return true;
    }

    public boolean logout() throws LoginException {
        return true;
    }

    protected void getUsernameAndPassword() throws LoginException {
        // prompt for a username and password
        if (callbackHandler == null) {
            throw new LoginException("Error: no CallbackHandler available " + "to collect authentication information");
        }

        NameCallback nc = new NameCallback("User name: ", "guest");
        PasswordCallback pc = new PasswordCallback("Password: ", false);
        Callback[] callbacks = {nc, pc};

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
    }
}
