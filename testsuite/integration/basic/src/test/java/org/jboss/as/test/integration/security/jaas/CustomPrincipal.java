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
package org.jboss.as.test.integration.security.jaas;

import java.security.Principal;

/**
 * A custom {@link Principal} implementation.
 *
 * @author Josef Cacek
 */
public class CustomPrincipal implements Principal {

    private final String name;

    // Constructors ----------------------------------------------------------

    /**
     * Create a new CustomPrincipal.
     *
     * @param name
     */
    public CustomPrincipal(final String name) {
        this.name = name;
    }

    // Public methods --------------------------------------------------------

    /**
     * Returns the Principal name.
     *
     * @return
     * @see java.security.Principal#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @param o
     * @return
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof CustomPrincipal)) {
            return false;
        }

        CustomPrincipal that = (CustomPrincipal) o;

        return getName().equals(that.getName());
    }

    /**
     * @return
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        //keep value returned the same as returned from SimplePrincipal.hashCode()
        return this.name.hashCode();
    }

}
