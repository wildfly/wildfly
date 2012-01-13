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

import java.security.Principal;
import java.security.acl.Group;

import javax.security.auth.login.LoginException;

import org.jboss.as.security.SecurityMessages;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.security.UserPrincipal;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.AbstractServerLoginModule;

/**
 * A simple LoginModule to take the UserPrincipal from the inbound Remoting connection and to use it as an already authenticated
 * user.
 *
 * Subsequent login modules can be chained after this module to load role information.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemotingLoginModule extends AbstractServerLoginModule {

    private static final Logger log = Logger.getLogger("org.jboss.as.security");

    private Principal identity;

    @Override
    public boolean login() throws LoginException {
        if (super.login() == true) {
            log.debug("super.login()==true");
            return true;
        }

        Connection con = RemotingContext.getConnection();
        if (con != null) {
            UserPrincipal up = null;
            for (Principal current : con.getPrincipals()) {
                if (current instanceof UserPrincipal) {
                    up = (UserPrincipal) current;
                }
                break;
            }
            // If we found a principal from the connection then authentication succeeded.
            if (up != null) {
                identity = up;
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
            } else {
                // Don't know of scenarios where we would have a connection but no UserPrinicpal so
                // completely fail the auth attempt.
                throw SecurityMessages.MESSAGES.remotingConnectionWithNoUserPrincipal();
            }
        }

        // We return false to allow the next module to attempt authentication, maybe a
        // username and password has been supplied to a web auth.
        return false;
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
