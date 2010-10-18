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
 * An update which removes a {@link ManagementElement} from a host element.
 *
 * @author Brian Stansberry
 */
public final class HostManagementSocketRemove extends AbstractHostModelUpdate<Void> {
    private static final long serialVersionUID = 6075488950873140885L;

    /**
     * Construct a new instance.
     */
    public HostManagementSocketRemove() {
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final HostModel element) throws UpdateFailedException {
        if (!element.removeManagementElement()) {
            throw new UpdateFailedException("No management socket configured");
        }
    }

    /** {@inheritDoc} */
    @Override
    public HostManagementSocketAdd getCompensatingUpdate(final HostModel original) {
        ManagementElement me = original.getManagementElement();
        if (me == null)
            return null;
        return new HostManagementSocketAdd(me.getInterfaceName(), me.getPort());
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }
}
