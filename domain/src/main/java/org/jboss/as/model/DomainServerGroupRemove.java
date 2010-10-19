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
 * Remove server group update.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainServerGroupRemove extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -7482118547411879295L;
    private final String serverGroupName;

    public DomainServerGroupRemove(String serverGroupName) {
        this.serverGroupName = serverGroupName;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(DomainModel element) throws UpdateFailedException {
        if(element.removeServerGroup(serverGroupName)) {
            throw new UpdateFailedException(String.format("server-group (%s) does not exist.", serverGroupName));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbstractDomainModelUpdate<?> getCompensatingUpdate(DomainModel original) {
        final ServerGroupElement group = original.getServerGroup(serverGroupName);
        return new DomainServerGroupAdd(group.getName(), group.getProfileName());
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        // FIXME -- figure out impact of removing server group on ServerModel
        // Basically should be none, but their should be validation that
        // group isn't used on a server before this update is applied
        return null;
    }

    @Override
    public List<String> getAffectedServers(DomainModel domainModel, HostModel hostModel) throws UpdateFailedException {
        StringBuilder illegalServers = null;
        for (ServerElement server : hostModel.getServers()) {
            if (serverGroupName.equals(server.getServerGroup())) {
                if (illegalServers == null) {
                    illegalServers = new StringBuilder(server.getName());
                }
                else {
                    illegalServers.append(", ");
                    illegalServers.append(server.getName());
                }
            }
        }

        if (illegalServers != null) {
            throw new UpdateFailedException(String.format("Cannot remove server group %s as it still has member servers %s", serverGroupName, illegalServers.toString()));
        }
        return Collections.emptyList();
    }

}
