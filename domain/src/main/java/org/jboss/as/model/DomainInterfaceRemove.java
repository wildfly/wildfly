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

import org.jboss.as.model.socket.InterfaceAdd;
import org.jboss.as.model.socket.InterfaceElement;

/**
 * Update removing a {@link InterfaceElement} from the {@link DomainModel}.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainInterfaceRemove extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -9182707456362234629L;
    private final String name;

    public DomainInterfaceRemove(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(DomainModel element) throws UpdateFailedException {
        if(!element.removeInterface(name)) {
            throw new UpdateFailedException("failed to remove network interface " + name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbstractDomainModelUpdate<?> getCompensatingUpdate(DomainModel original) {
        final InterfaceElement element = original.getInterface(name);
        if(element == null) {
            return null;
        }
        return new DomainInterfaceAdd(new InterfaceAdd(element));
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerModelInterfaceRemove(name);
    }

}
