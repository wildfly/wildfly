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
 * An update which removes a {@link ServerElement} from a host element.
 *
 * @author Brian Stansberry
 */
public final class HostServerRemove extends AbstractHostModelUpdate<Void> {
    private static final long serialVersionUID = 6075488950873140885L;

    private final String serverName;

    /**
     * Construct a new instance.
     *
     * @param serverName the name of the server
     */
    public HostServerRemove(final String serverName) {
        this.serverName = serverName;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final HostModel element) throws UpdateFailedException {
        if (!element.removeServer(serverName)) {
            throw new UpdateFailedException("Server " + serverName + " was not configured");
        }
    }

    /** {@inheritDoc} */
    @Override
    public HostServerAdd getCompensatingUpdate(final HostModel original) {
        ServerElement se = original.getServer(serverName);
        if (se != null)
            return new HostServerAdd(se.getName(), se.getServerGroup());
        else return null;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }
}
