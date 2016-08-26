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

package org.jboss.as.security;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.wildfly.security.sasl.util.UsernamePasswordHashUtil;

/**
 * @author Jason T. Greene
 */
public class RealmUsersRolesLoginModule extends UsersRolesLoginModule {
    private static final String DEFAULT_REALM = "ApplicationRealm";
    private static final String REALM_OPTION = "realm";

    private static final String[] ALL_VALID_OPTIONS = {
                    REALM_OPTION
            };

    private UsernamePasswordHashUtil usernamePasswordHashUtil;
    private String realm;

    public RealmUsersRolesLoginModule() {
        try {
            usernamePasswordHashUtil = new UsernamePasswordHashUtil();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        addValidOptions(ALL_VALID_OPTIONS);

        this.realm = options.containsKey(REALM_OPTION) ? (String) options.get(REALM_OPTION) : DEFAULT_REALM;
        HashMap map = new HashMap(options);
        map.putAll(options);
        map.put("hashAlgorithm", "REALM");
        map.put("hashStorePassword", "false");
        super.initialize(subject, callbackHandler, sharedState, map);
    }

    @Override
    protected String createPasswordHash(String username, String password, String digestOption) throws LoginException {
        return usernamePasswordHashUtil.generateHashedHexURP(username, realm, password.toCharArray());
    }
}
