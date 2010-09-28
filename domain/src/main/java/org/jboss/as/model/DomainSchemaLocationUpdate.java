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

import java.util.List;

/**
 * An update which changes the schema locations on a domain element.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainSchemaLocationUpdate extends AbstractDomainModelUpdate<Void> {
    private static final long serialVersionUID = 6075488950873140885L;

    private final List<SchemaLocation> locations;

    /**
     * Construct a new instance.
     *
     * @param locations the list of prefixes to apply
     */
    public DomainSchemaLocationUpdate(final List<SchemaLocation> locations) {
        this.locations = locations;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        element.setSchemaLocations(locations);
    }

    /** {@inheritDoc} */
    public DomainSchemaLocationUpdate getCompensatingUpdate(final DomainModel original) {
        return new DomainSchemaLocationUpdate(original.getSchemaLocations());
    }

    /** {@inheritDoc} */
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }
}
