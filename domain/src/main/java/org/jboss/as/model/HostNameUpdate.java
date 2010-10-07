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


/**
 * An update which changes the name property on a host element.
 *
 * @author Brian Stansberry
 */
public final class HostNameUpdate extends AbstractHostModelUpdate<Void> {
    private static final long serialVersionUID = 6075488950873140885L;

    private final String name;

    /**
     * Construct a new instance.
     *
     * @param name the name of the host
     */
    public HostNameUpdate(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final HostModel element) throws UpdateFailedException {
        element.setName(name);
    }

    /** {@inheritDoc} */
    @Override
    public HostNameUpdate getCompensatingUpdate(final HostModel original) {
        return new HostNameUpdate(original.getName());
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }
}
