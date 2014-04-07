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

package org.jboss.as.domain.management.security;

import java.net.URI;


/**
 * Representation of an entry in LDAP by both it's simple name and distinguished name.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
final class LdapEntry {

    private final String simpleName;
    private final String distinguishedName;

    private final LdapEntry previousEntry;
    private final URI referralURI;

    private final int hashCode;

    LdapEntry(final String simpleName, final String distinguishedName) {
        this(simpleName, distinguishedName, null, null);
    }

    LdapEntry(final String simpleName, final String distinguishedName, final URI referralURI) {
        this(simpleName, distinguishedName, null, referralURI);
    }

    LdapEntry(final String simpleName, final String distinguishedName, final LdapEntry previousEntry, final URI referralURI) {
        this.simpleName = simpleName;
        this.distinguishedName = distinguishedName;

        this.previousEntry = previousEntry;
        this.referralURI = referralURI;

        hashCode = (simpleName == null ? 7 : simpleName.hashCode())
                * (distinguishedName == null ? 31 : distinguishedName.hashCode()
                * (previousEntry == null ? 37 : previousEntry.hashCode)
                * (referralURI == null ? 41 : referralURI.hashCode()));
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public LdapEntry getPreviousEntry() {
        return previousEntry;
    }

    public URI getReferralUri() {
        return referralURI;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LdapEntry ? equals((LdapEntry) obj) : false;
    }

    private boolean equals(LdapEntry obj) {
        if (obj == this) {
            return true;
        }
        return (obj.hashCode == hashCode)
                && (simpleName == null ? obj.simpleName == null : simpleName.equals(obj.simpleName))
                && (distinguishedName == null ? obj.distinguishedName == null : distinguishedName.equals(obj.distinguishedName)
                && (previousEntry == null ? obj.previousEntry == null : previousEntry.equals(obj.previousEntry))
                && (referralURI == null ? obj.referralURI == null : referralURI.equals(obj.referralURI)));
    }

    @Override
    public String toString() {
        return String.format("LdapEntry simpleName='%s', distinguishedName='%s', previousEntry='%s', referralConnection='%s'", simpleName, distinguishedName, previousEntry, referralURI);
    }

}
