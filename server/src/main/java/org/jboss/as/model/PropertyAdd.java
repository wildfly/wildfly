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

package org.jboss.as.model;

import java.util.Map;
import java.util.Properties;

/**
 * An update which adds a property to a property list.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PropertyAdd extends AbstractPropertyUpdate {

    private static final long serialVersionUID = 5040034824081445679L;

    private final String value;

    /**
     * Construct a new instance.
     *
     * @param name the property name
     * @param value the property value
     */
    public PropertyAdd(final String name, final String value) {
        super(name);
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final PropertiesElement element) {
        element.addProperty(getPropertyName(), value);
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final Properties properties) throws UpdateFailedException {
        properties.setProperty(getPropertyName(), value);
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final Map<? super String, ? super String> map) {
        map.put(getPropertyName(), value);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractPropertyUpdate getCompensatingUpdate(final PropertiesElement original) {
        return new PropertyRemove(getPropertyName());
    }

    /**
     * Get the property value to be added.
     *
     * @return the property value
     */
    public String getValue() {
        return value;
    }
}
