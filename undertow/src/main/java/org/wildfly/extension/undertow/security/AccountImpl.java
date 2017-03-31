/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.security;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.undertow.security.idm.Account;

/**
 *
 * @author Stuart Douglas
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AccountImpl implements Account, Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Set<String> roles = new CopyOnWriteArraySet<>();
    private final Principal principal;
    private final Object credential;

    private final Principal originalPrincipal;

    public AccountImpl(final String name) {
        this.name = name;
        this.principal = new AccountPrincipal(name);
        this.credential = null;
        this.originalPrincipal = null;
    }

    public AccountImpl(final Principal principal) {
        this.principal = principal;
        this.name = principal.getName();
        this.credential = null;
        this.originalPrincipal = null;
    }
    public AccountImpl(final Principal principal, Set<String> roles, final Object credential, Principal originalPrincipal) {
        this.principal = principal;
        this.credential = credential;
        this.originalPrincipal = originalPrincipal;
        this.name = principal.getName();
        this.roles.addAll(roles);
    }

    public AccountImpl(final Principal principal, Set<String> roles, final Object credential) {
        this.principal = principal;
        this.credential = credential;
        this.originalPrincipal = null;
        this.name = principal.getName();
        this.roles.addAll(roles);
    }

    void setRoles(final Set<String> roles) {
        this.roles.clear();
        roles.addAll(roles);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final AccountImpl account = (AccountImpl) o;

        if (name != null ? !name.equals(account.name) : account.name != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    /**
     * If the original principal was set then this will be returned, otherwise
     * it will return the current principal.
     *
     * If principal mapping is used the principal for the verified account can be different
     * to the principal that need to be used for authentication. When calling
     * {@link io.undertow.security.idm.IdentityManager#verify(io.undertow.security.idm.Account)}
     * for an existing account this is the principal that must be used.
     *
     * see UNDERTOW-273
     * @return The original principal
     */
    public Principal getOriginalPrincipal() {
        if (originalPrincipal != null) {
            return originalPrincipal;
        }
        return principal;
    }

    public Object getCredential() {
        return credential;
    }

    private static class AccountPrincipal implements Principal, Serializable {

        private static final long serialVersionUID = 1L;
        private String name;

        public AccountPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AccountPrincipal ? equals((AccountPrincipal) obj) : false;
        }

        private boolean equals(AccountPrincipal other) {
            return name.equals(other.getName());
        }

    }
}
