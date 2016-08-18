/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.container.interceptor.security;

import java.security.Principal;
import java.security.acl.Group;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;

import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.callback.ObjectCallback;
import org.jboss.security.auth.spi.AbstractServerLoginModule;

/**
 * Login module which allows the "guest" user to switch to another identity.
 *
 * @author Josef Cacek
 */
public class GuestDelegationLoginModule extends AbstractServerLoginModule {

    private Principal identity;

    // Public methods --------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public boolean login() throws LoginException {
        if (super.login() == true) {
            log.debug("super.login()==true");
            return true;
        }

        // Time to see if this is a delegation request.
        NameCallback ncb = new NameCallback("Username:");
        ObjectCallback ocb = new ObjectCallback("Password:");

        try {
            callbackHandler.handle(new Callback[] { ncb, ocb });
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            return false; // If the CallbackHandler can not handle the required callbacks then no chance.
        }

        String name = ncb.getName();
        Object credential = ocb.getCredential();

        if (credential instanceof CurrentUserCredential) {
            // This credential type will only be seen for a delegation request, if not seen then the request is not for us.

            final CurrentUserCredential cuCredential = (CurrentUserCredential) credential;
            // only the "guest" can be switched to another identity
            if ("guest".equals(cuCredential.getUser())) {

                identity = new SimplePrincipal(name);
                if (getUseFirstPass()) {
                    String userName = identity.getName();
                    if (log.isDebugEnabled())
                        log.debug("Storing username '" + userName + "' and empty password");
                    // Add the username and an empty password to the shared state map
                    sharedState.put("javax.security.auth.login.name", identity);
                    sharedState.put("javax.security.auth.login.password", "");
                }
                loginOk = true;
                return true;
            }
        }

        return false; // Attempted login but not successful.
    }

    // Protected methods -----------------------------------------------------

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
