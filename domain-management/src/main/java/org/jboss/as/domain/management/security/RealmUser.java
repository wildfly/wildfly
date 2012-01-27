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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * The Principal used to represent the name of an authenticated user.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RealmUser extends DomainManagementPrincipal {

    private final String realm;

    public RealmUser(final String realm, final String name) {
        super(name);
        if (name == null) {
            throw MESSAGES.canNotBeNull("realm");
        }
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public String getFullName() {
        return getName() + "@" + realm;
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public int hashCode() {
        return (super.hashCode() + 31) * realm.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RealmUser ? equals((RealmUser) obj) : false;

    }

    private boolean equals(RealmUser user) {
        return this == user ? true : super.equals(user) && realm.equals(user.realm);
    }

}
