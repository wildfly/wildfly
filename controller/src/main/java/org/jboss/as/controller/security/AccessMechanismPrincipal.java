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
package org.jboss.as.controller.security;

import java.io.Serializable;
import java.security.Principal;

import org.jboss.as.core.security.AccessMechanism;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AccessMechanismPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -3233777258697207164L;

    private final AccessMechanism accessMechanism;

    public AccessMechanismPrincipal(AccessMechanism accessMechanism) {
        this.accessMechanism = accessMechanism;
    }

    /**
     * Get the name of this principal; it will be the string representation of the access mechanism
     *
     * @return the name of this principal
     */
    @Override
    public String getName() {
        return accessMechanism.name();
    }

    /**
     * Get the IP address of this principal.
     *
     * @return the address
     */
    public AccessMechanism getAccessMechanism() {
        return accessMechanism;
    }

    /**
     * Determine whether this instance is equal to another.
     *
     * @param other the other instance
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final Object other) {
        return accessMechanism.equals(other);
    }


    /**
     * Get the hash code for this instance.  It will be equal to the hash code of the {@code InetAddress} object herein.
     *
     * @return the hash code
     */
    public int hashCode() {
        return accessMechanism.hashCode();
    }

    /**
     * Get a human-readable representation of this principal.
     *
     * @return the string
     */
    public String toString() {
        return accessMechanism.toString();
    }

}
