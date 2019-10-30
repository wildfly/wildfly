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

/**
 * A helper Credential, which holds current RealmUser name. It's used in the {@link GuestDelegationLoginModule} to check if the
 * delegation is allowed for this user.
 *
 * @author Josef Cacek
 */
public final class CurrentUserCredential {

    private final String user;

    public CurrentUserCredential(final String user) {
        if (user == null) {
            throw new IllegalArgumentException("User can not be null.");
        }
        this.user = user;
    }

    // Public methods --------------------------------------------------------

    /**
     * Returns user name held by this Credential.
     *
     * @return
     */
    public String getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return user.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CurrentUserCredential
                && (this == other || other != null && user.equals(((CurrentUserCredential) other).user));
    }

}
