/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * An element representing a list of properties (name/value pairs).
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PropertiesElement {

    private static final long serialVersionUID = 1614693052895734582L;

    private final Map<String, String> properties = new LinkedHashMap<String, String>();
    private final Element propertyType;
    private final boolean allowNullValue;

    /**
     * Construct a new instance.
     *
     */
    public PropertiesElement(final Element propertyType, final boolean allowNullValue) {
        this.propertyType = propertyType;
        this.allowNullValue = allowNullValue;
    }

    public PropertiesElement(final Element propertyType, boolean allowNullValue, PropertiesElement ... toCombine) {
        this.allowNullValue = allowNullValue;
        this.propertyType = propertyType;
        if (toCombine != null) {
            for (PropertiesElement pe : toCombine) {
                if (pe == null)
                    continue;
                for (String name : pe.getPropertyNames()) {
                    String val = pe.getProperty(name);
                    if (!allowNullValue && val == null) {
                        throw MESSAGES.propertyValueHasNullValue(name);
                    }
                    else {
                        properties.put(name, val);
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long total = 0;
        synchronized (properties) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String val = entry.getValue();
                int valHash = val == null ? 0 : val.hashCode();
                total = Long.rotateLeft(total, 1) ^ ((long)entry.getKey().hashCode() << 32L | valHash & 0xffffffffL);
            }
        }
        return total;
    }

    void addProperty(final String name, final String value) {
        synchronized (properties) {
            if (properties.containsKey(name)) {
                throw MESSAGES.propertyAlreadyExists(name);
            }
            if (value == null && !allowNullValue) {
                throw MESSAGES.propertyValueNull(name);
            }
            properties.put(name, value);
        }
    }

    String removeProperty(final String name) {
        synchronized (properties) {
            final String old = properties.remove(name);
            if (old == null) {
                throw MESSAGES.propertyNotFound(name);
            }
            return old;
        }
    }

    public int size() {
        return properties.size();
    }

    /**
     * Get the value of a property defined in this element.
     *
     * @param name the property name
     * @return the value, or {@code null} if the property does not exist
     */
    public String getProperty(final String name) {
        return properties.get(name);
    }

    /**
     * Gets the names of the properties.
     *
     * @return the names. Will not return <code>null</code>
     */
    public Set<String> getPropertyNames() {
        return new HashSet<String>(properties.keySet());
    }

    /**
     * Get a copy of the properties map.
     *
     * @return the copy of the properties map
     */
    public Map<String, String> getProperties() {
        return new HashMap<String, String>(properties);
    }
}
