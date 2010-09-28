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
 * An update which applies to a property list.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractPropertyUpdate extends AbstractModelElementUpdate<PropertiesElement> {

    private static final long serialVersionUID = -988127260524571932L;

    /**
     * Construct a new instance.
     */
    protected AbstractPropertyUpdate() {
    }

    /** {@inheritDoc} */
    public final Class<PropertiesElement> getModelElementType() {
        return PropertiesElement.class;
    }

    /** {@inheritDoc} */
    protected abstract void applyUpdate(PropertiesElement element) throws UpdateFailedException;

    /**
     * Apply this update to an actual set of properties.
     *
     * @param properties the properties to apply the update to
     * @throws UpdateFailedException if the update fails for some reason
     */
    protected abstract void applyUpdate(Properties properties) throws UpdateFailedException;

    /**
     * Apply this update to a plain map.
     *
     * @param map the map
     */
    protected abstract void applyUpdate(Map<? super String, ? super String> map);

    /** {@inheritDoc} */
    public abstract AbstractPropertyUpdate getCompensatingUpdate(PropertiesElement original);
}
