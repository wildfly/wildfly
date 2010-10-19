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

import java.util.Collections;
import java.util.List;

/**
 * An update which changes a {@link ManagementElement}'s max-threads setting.
 *
 * @author Brian Stansberry
 */
public final class HostManagementSocketThreadsUpdate extends AbstractHostModelUpdate<Void> {

    private static final long serialVersionUID = 6075488950873140885L;

    private final int maxThreads;

    /**
     * Construct a new instance.
     */
    public HostManagementSocketThreadsUpdate(final int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final HostModel element) throws UpdateFailedException {
        ManagementElement me = element.getManagementElement();
        if (me == null) {
            throw new UpdateFailedException("No management socket configured");
        }
        me.setMaxThreads(maxThreads);
    }

    /** {@inheritDoc} */
    @Override
    public HostManagementSocketThreadsUpdate getCompensatingUpdate(final HostModel original) {
        ManagementElement me = original.getManagementElement();
        if (me == null)
            return null;
        return new HostManagementSocketThreadsUpdate(me.getMaxThreads());
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }

    @Override
    public List<String> getAffectedServers(HostModel hostModel) {
        return Collections.emptyList();
    }
}
