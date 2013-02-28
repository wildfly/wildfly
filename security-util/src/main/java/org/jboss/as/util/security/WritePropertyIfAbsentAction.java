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
import java.util.Properties;

/**
 * A privileged action for setting a system property if it is absent.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class WritePropertyIfAbsentAction implements PrivilegedAction<String> {
    private final String propertyName;
    private final String value;

    /**
     * Construct a new instance.
     *
     * @param propertyName the property name to set
     * @param value the value to use
     */
    public WritePropertyIfAbsentAction(final String propertyName, final String value) {
        this.propertyName = propertyName;
        this.value = value;
    }

    public String run() {
        final Properties properties = System.getProperties();
        synchronized (properties) {
            if (properties.containsKey(propertyName)) {
                return properties.getProperty(propertyName);
            } else {
                return (String) properties.setProperty(propertyName, value);
            }
        }
    }
}
