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

package org.jboss.as.test.integration.security.common;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.jboss.security.SecurityConstants;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.spi.AbstractServerLoginModule;

/**
 * Login module, which adds a single role (configured as {@value #ROLE_NAME} module option) to an already authenticated
 * identity. It can be plugged into a login module stack, where already exists an authenticating login module with
 * password-stacking=useFirstPass module option.
 * <p>
 * Example configuration:
 *
 * <pre>
 * &lt;security-domain name=&quot;SPNEGO&quot; cache-type=&quot;default&quot;&gt;
 *     &lt;authentication&gt;
 *         &lt;login-module code=&quot;SPNEGO&quot; flag=&quot;required&quot;&gt;
 *             &lt;module-option name=&quot;serverSecurityDomain&quot; value=&quot;host&quot;/&gt;
 *             &lt;module-option name=&quot;password-stacking&quot; value=&quot;useFirstPass&quot;/&gt;
 *         &lt;/login-module&gt;
 *         &lt;login-module code=&quot;org.jboss.AddRoleLoginModule&quot; flag=&quot;optional&quot;&gt;
 *             &lt;module-option name=&quot;roleName&quot; value=&quot;admin&quot;/&gt;
 *             &lt;module-option name=&quot;password-stacking&quot; value=&quot;useFirstPass&quot;/&gt;
 *         &lt;/login-module&gt;
 *     &lt;/authentication&gt;
 * &lt;/security-domain&gt;
 * </pre>
 *
 * @author Josef Cacek
 */
public class AddRoleLoginModule extends AbstractServerLoginModule {

    private static final String ROLE_NAME = "roleName";
    private static final String[] ALL_VALID_OPTIONS = { ROLE_NAME };

    private String role;
    private Principal identity;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        addValidOptions(ALL_VALID_OPTIONS);
        super.initialize(subject, callbackHandler, sharedState, options);
        role = (String) options.get(ROLE_NAME);
    }

    @Override
    public boolean login() throws LoginException {
        if (super.login()) {
            Object username = sharedState.get("javax.security.auth.login.name");
            if (username instanceof Principal)
                identity = (Principal) username;
            else {
                String name = username.toString();
                try {
                    identity = createIdentity(name);
                } catch (Exception e) {
                    LoginException le = new LoginException("Identity creation failed");
                    le.initCause(e);
                    throw new LoginException("Identity");
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        Group roles = new SimpleGroup(SecurityConstants.ROLES_IDENTIFIER);
        roles.addMember(new SimplePrincipal(role));
        return new Group[] { roles };
    }

    @Override
    protected Principal getIdentity() {
        return identity;
    }

}
