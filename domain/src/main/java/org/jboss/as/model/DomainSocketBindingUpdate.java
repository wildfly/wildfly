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

import org.jboss.as.model.socket.SocketBindingGroupElement;

/**
 * Domain level {@code SocketBindingElement} update.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainSocketBindingUpdate extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = 1L;

    private final String socketGroupName;
    private final AbstractSocketBindingUpdate update;

    public DomainSocketBindingUpdate(String socketGroupName, AbstractSocketBindingUpdate udpate) {
        this.socketGroupName = socketGroupName;
        this.update = udpate;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(DomainModel element) throws UpdateFailedException {
        final SocketBindingGroupElement group = element.getSocketBindingGroup(socketGroupName);
        if(group == null) {
            throw new UpdateFailedException("no such socket-binding group " + socketGroupName);
        }
        update.applyUpdate(group);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractDomainModelUpdate<?> getCompensatingUpdate(DomainModel original) {
        final SocketBindingGroupElement group = original.getSocketBindingGroup(socketGroupName);
        return new DomainSocketBindingUpdate(socketGroupName, update.getCompensatingUpdate(group));
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerSocketBindingUpdate(update);
    }

}
