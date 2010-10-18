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
 * Update a {@link ServerGroupElement} within a {@link DomainModel}.
 *
 * @author Brian Stansberry
 */
public final class DomainServerGroupUpdate<R> extends AbstractDomainModelUpdate<R> {

    private static final long serialVersionUID = -9076890219875153928L;

    private final String serverGroupName;
    private final AbstractModelUpdate<ServerGroupElement, R> serverGroupUpdate;


    public static <T> DomainServerGroupUpdate<T> create(final String serverGroupName, AbstractModelUpdate<ServerGroupElement, T> update) {
        return new DomainServerGroupUpdate<T>(serverGroupName, update);
    }

    /**
     * Construct a new instance.
     *
     * @param serverGroupName the name of the profile that the change applies to
     * @param serverGroupUpdate the update to the server group
     */
    public DomainServerGroupUpdate(final String serverGroupName, final AbstractModelUpdate<ServerGroupElement, R> serverGroupUpdate) {
        this.serverGroupName = serverGroupName;
        this.serverGroupUpdate = serverGroupUpdate;
    }

    /**
     * Get the name of the server group to be updated.
     *
     * @return the profile name
     */
    public String getServerGroupName() {
        return serverGroupName;
    }

    @Override
    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        final ServerGroupElement serverGroup = element.getServerGroup(serverGroupName);
        if (serverGroup == null) {
            throw new UpdateFailedException("Server group " + serverGroupName + " does not exist");
        }
        serverGroupUpdate.applyUpdate(serverGroup);
    }

    @Override
    public DomainServerGroupUpdate<?> getCompensatingUpdate(final DomainModel original) {
        final ServerGroupElement serverGroup = original.getServerGroup(serverGroupName);
        if (serverGroup == null)
            return null;
        return create(serverGroupName, serverGroupUpdate.getCompensatingUpdate(serverGroup));
    }

    @Override
    public AbstractServerModelUpdate<R> getServerModelUpdate() {
        // FIXME hmmm
        return serverGroupUpdate.getServerModelUpdate();
    }
}
