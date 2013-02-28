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

package org.jboss.as.util.security;

import java.security.PrivilegedAction;

/**
 * A privileged action for reading a system property.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ReadPropertyAction implements PrivilegedAction<String> {
    private final String propertyName;
    private final String defaultValue;

    /**
     * Construct a new instance.
     *
     * @param propertyName the property name to read
     */
    public ReadPropertyAction(final String propertyName) {
        this(propertyName, null);
    }

    /**
     * Construct a new instance.
     *
     * @param propertyName the property name to read
     * @param defaultValue the value to use if the property is not present ({@code null} for none)
     */
    public ReadPropertyAction(final String propertyName, final String defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    public String run() {
        return defaultValue == null ? System.getProperty(propertyName) : System.getProperty(propertyName, defaultValue);
    }
}
