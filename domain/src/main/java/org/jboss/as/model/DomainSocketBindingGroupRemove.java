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
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.as.model.socket.SocketBindingGroupUpdate;

/**
 * @author Emanuel Muckenhuber
 */
public class DomainSocketBindingGroupRemove extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = 1L;
    private final String bindingGroupName;

    public DomainSocketBindingGroupRemove(final String bindingGroupName) {
        this.bindingGroupName = bindingGroupName;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(DomainModel element) throws UpdateFailedException {
        StringBuilder illegal = null;
        for (String groupName : element.getServerGroupNames()) {
            ServerGroupElement sge = element.getServerGroup(groupName);
            if (bindingGroupName.equals(sge.getSocketBindingGroupName())) {
                if (illegal == null) {
                    illegal = new StringBuilder(groupName);
                }
                else {
                    illegal.append(", ");
                    illegal.append(groupName);
                }
            }
        }

        if (illegal != null) {
            throw new UpdateFailedException(String.format("Socket binding group %s cannot " +
                    "be removed as it is referenced by server groups %s",
                    this.bindingGroupName, illegal.toString()));
        }

        if(! element.removeBindingGroup(bindingGroupName)) {
            throw new UpdateFailedException(String.format("binding-group (%s) does not exist", bindingGroupName));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbstractDomainModelUpdate<?> getCompensatingUpdate(DomainModel domain) {
        final SocketBindingGroupElement original = domain.getSocketBindingGroup(bindingGroupName);
        final SocketBindingGroupUpdate update = new SocketBindingGroupUpdate(
                bindingGroupName,
                original.getDefaultInterface(),
                original.getIncludedSocketBindingGroups());
        return new DomainSocketBindingGroupAdd(update);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }

    @Override
    public List<String> getAffectedServers(DomainModel domainModel, HostModel hostModel) throws UpdateFailedException {

        // Validate no servers are directly using this group
        StringBuilder illegal = null;
        for (ServerElement server : hostModel.getServers()) {
            if (bindingGroupName.equals(server.getSocketBindingGroupName())) {
                if (illegal == null) {
                    illegal = new StringBuilder(server.getName());
                }
                else {
                    illegal.append(", ");
                    illegal.append(server.getName());
                }
            }
        }

        if (illegal != null) {
            throw new UpdateFailedException(String.format("Socket binding group %s cannot " +
                    "be removed as it is referenced by servers %s",
                    this.bindingGroupName, illegal.toString()));
        }

        return Collections.emptyList();
    }
}
