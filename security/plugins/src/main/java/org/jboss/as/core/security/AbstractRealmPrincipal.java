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

package org.jboss.as.core.security;

import static org.wildfly.common.Assert.checkNotNullParam;

/**
 * A base {@link Principal} where a realm is also associated.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
abstract class AbstractRealmPrincipal extends SecurityRealmPrincipal implements RealmPrincipal {

    private static final long serialVersionUID = -5558581540228214884L;

    private int hashBase = this.getClass().getName().hashCode();
    private final String realm;

    public AbstractRealmPrincipal(final String name) {
        super(name);
        this.realm = null;
    }

    public AbstractRealmPrincipal(final String realm, final String name) {
        super(name);
        this.realm = checkNotNullParam("realm", realm);
    }

    public String getRealm() {
        return realm;
    }

    public String getFullName() {
        return realm == null ? getName() : getName() + "@" + realm;
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public int hashCode() {
        return (super.hashCode() + hashBase) * (realm == null ? 101 : realm.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass()) ? equals((AbstractRealmPrincipal) obj) : false;

    }

    private boolean equals(AbstractRealmPrincipal user) {
        return (this == user ? true : super.equals(user)) && (realm == null ? user.realm == null : realm.equals(user.realm));
    }

}