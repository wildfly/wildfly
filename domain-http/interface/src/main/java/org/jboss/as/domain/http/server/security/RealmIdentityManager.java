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
package org.jboss.as.domain.http.server.security;

import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.SecurityRealm;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;

/**
 * {@link IdentityManager} implementation to wrap the current security realms.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RealmIdentityManager implements IdentityManager {

    private static final ThreadLocal<AuthMechanism> currentMechanism = new ThreadLocal<AuthMechanism>();

    static void setAuthenticationMechanism(final AuthMechanism mechanism) {
        currentMechanism.set(mechanism);
    }

    private final SecurityRealm securityRealm;

    public RealmIdentityManager(final SecurityRealm securityRealm) {
        this.securityRealm = securityRealm;
    }

    @Override
    public Account verify(Account account) {
        throw new RuntimeException("Not implemented for domain management.");
    }

    /*
     * The next pair of methods would typically be used for Digest style authentication.
     */

    @Override
    public Account getAccount(String id) {
        return null;
    }

    @Override
    public char[] getPassword(Account account) {
        return null;
    }

    /*
     * The single method is used for BASIC authentication to perform validation in a single step.
     */

    @Override
    public Account verify(String id, Credential credential) {
        return null;
    }

    /*
     * The final single method is used for Client Cert style authentication only.
     */

    @Override
    public Account verify(Credential credential) {
        return null;
    }

}
