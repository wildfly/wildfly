/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.securitydomain.module;

import java.security.acl.Group;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;

/**
 * @author Francesco Marchioni
 */
public class NonValidatingLoginModule extends UsernamePasswordLoginModule {

    public void initialize(Subject theSubject, CallbackHandler callbackHdlr, Map<String, ?> shrdState, Map<String, ?> opts) {
        super.initialize(theSubject, callbackHdlr, shrdState, opts);
    }

    public boolean login() throws LoginException {
        System.out.println("login called");
        if (super.login()) {
            return true;
        }
        loginOk = true;
        return true;
    }

    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        return true;
    }

    protected String getUsersPassword() throws LoginException {
        return getUsernameAndPassword()[1];
    }

    protected Group[] getRoleSets() throws LoginException {
        Group[] groups = { new SimpleGroup("Roles"), new SimpleGroup("CallerPrincipal") };
        groups[1].addMember(new MyPrincipal(getUsername()));
        return groups;
    }
}
