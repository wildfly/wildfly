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
 * An update which adds a {@link ServerElement} to a host element.
 *
 * @author Brian Stansberry
 */
public final class HostServerAdd extends AbstractHostModelUpdate<Void> {
    private static final long serialVersionUID = 6075488950873140885L;

    private final String serverName;
    private final String groupName;

    /**
     * Construct a new instance.
     *
     * @param serverName the name of the server
     * @param groupName the name of the server group the server is a member of
     */
    public HostServerAdd(final String serverName, final String groupName) {
        this.serverName = serverName;
        this.groupName = groupName;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final HostModel element) throws UpdateFailedException {
        if (!element.addServer(serverName, groupName)) {
            throw new UpdateFailedException("Server " + serverName + " already configured");
        }
    }

    /** {@inheritDoc} */
    @Override
    public HostServerRemove getCompensatingUpdate(final HostModel original) {
        if (original.getServer(serverName) != null)
            return null;
        return new HostServerRemove(serverName);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }
}
