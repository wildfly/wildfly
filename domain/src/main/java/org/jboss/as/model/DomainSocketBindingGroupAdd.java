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
import org.jboss.as.model.socket.SocketBindingGroupUpdate;

/**
 * @author Emanuel Muckenhuber
 */
public class DomainSocketBindingGroupAdd extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = 6748745991529958637L;
    private final SocketBindingGroupUpdate update;

    public DomainSocketBindingGroupAdd(SocketBindingGroupUpdate update) {
        this.update = update;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(DomainModel element) throws UpdateFailedException {
        final SocketBindingGroupElement bindingGroup = element.addSocketBindingGroup(update.getName());
        if(bindingGroup == null) {
            throw new UpdateFailedException("duplicate binding-group " + update.getName());
        }
        update.applyUpdate(bindingGroup);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractDomainModelUpdate<?> getCompensatingUpdate(DomainModel domain) {
        return new DomainSocketBindingGroupRemove(update.getName());
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        // TODO Auto-generated method stub
        return null;
    }

}
