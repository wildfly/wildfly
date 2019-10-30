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
package org.jboss.as.test.integration.security.jaas;

import java.security.acl.Group;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.jboss.logging.Logger;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;

/**
 * A custom LoginModule which validates all users password against {@value #PASSWORD} value.
 *
 * @author Josef Cacek
 */
public class CustomLoginModule extends UsernamePasswordLoginModule {

    private static Logger LOGGER = Logger.getLogger(CustomLoginModule.class);

    public static final String PASSWORD = "test";
    public static final String CALLER_NAME = "Caller";

    public static final String MODULE_OPTION_ROLE = "role";

    public static int loginCounter = 0;

    private String role;

    /**
     * Initialization - takes {@value #MODULE_OPTION_ROLE} module option with name of role which should be assigned to all
     * authenticated users.
     *
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#initialize(javax.security.auth.Subject,
     * javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        role = (String) options.get(MODULE_OPTION_ROLE);
        super.initialize(subject, callbackHandler, sharedState, options);
    }

    /**
     * Login method only increases {@link #loginCounter} and calls login from the parent class.
     *
     * @return
     * @throws LoginException
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#login()
     */
    @Override
    public boolean login() throws LoginException {
        LOGGER.trace("Inside CustomLoginModule.login()");
        loginCounter++;
        return super.login();
    }

    /**
     * Returns Roles and CallerPrincipal groups. The Roles group contains role defined as login module option. The
     * CallerPrincipal contains {@link CustomPrincipal} instance with fixed name {@value #CALLER_NAME}.
     *
     * @return
     * @throws LoginException
     * @see org.jboss.security.auth.spi.AbstractServerLoginModule#getRoleSets()
     */
    @Override
    protected Group[] getRoleSets() throws LoginException {
        try {
            Group roles = new SimpleGroup(SecurityConstants.ROLES_IDENTIFIER);
            roles.addMember(new SimplePrincipal(role));
            Group callerPrincipal = new SimpleGroup(SecurityConstants.CALLER_PRINCIPAL_GROUP);
            callerPrincipal.addMember(new CustomPrincipal(CALLER_NAME));
            return new Group[]{roles, callerPrincipal};
        } catch (Exception e) {
            throw new LoginException(e.toString());
        }
    }

    /**
     * Returns {@value #PASSWORD}
     *
     * @return
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#getUsersPassword()
     */
    @Override
    protected String getUsersPassword() {
        return PASSWORD;
    }
}