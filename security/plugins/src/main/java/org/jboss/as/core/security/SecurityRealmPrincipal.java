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

package org.jboss.as.core.security;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.io.Serializable;
import java.security.Principal;

/**
 * Base class for Principals defined for security realms.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class SecurityRealmPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 3616079359863450698L;

    private final String name;

    SecurityRealmPrincipal(final String name) {
        this.name = checkNotNullParam("name", name);
    }

    /**
     * @see java.security.Principal#getName()
     */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass()) ? equals((SecurityRealmPrincipal) obj) : false;
    }

    protected boolean equals(SecurityRealmPrincipal principal) {
        return this == principal || name.equals(principal.name);
    }

    @Override
    public String toString() {
        return name;
    }

}