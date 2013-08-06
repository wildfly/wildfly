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
package org.jboss.as.controller.security;

import java.net.InetAddress;
import java.security.Principal;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class InetAddressPrincipal implements Principal {
    private final org.jboss.remoting3.security.InetAddressPrincipal delegate;

    public InetAddressPrincipal(org.jboss.remoting3.security.InetAddressPrincipal delegate) {
        this.delegate = delegate;
    }

    /**
     * Get the name of this principal; it will be the string representation of the IP address.
     *
     * @return the name of this principal
     */
    @Override
    public String getName() {
        return delegate.getName();
    }

    /**
     * Get the IP address of this principal.
     *
     * @return the address
     */
    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    /**
     * Determine whether this instance is equal to another.
     *
     * @param other the other instance
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final Object other) {
        return delegate.equals(other);
    }


    /**
     * Get the hash code for this instance.  It will be equal to the hash code of the {@code InetAddress} object herein.
     *
     * @return the hash code
     */
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * Get a human-readable representation of this principal.
     *
     * @return the string
     */
    public String toString() {
        return delegate.toString();
    }

}
