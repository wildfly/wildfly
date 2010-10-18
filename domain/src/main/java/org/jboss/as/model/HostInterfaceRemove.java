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
 * Update removing a {@link InterfaceElement} from the {@link HostModel}.
 *
 * @author Emanuel Muckenhuber
 */
public class HostInterfaceRemove extends AbstractHostModelUpdate<Void> {

    private static final long serialVersionUID = -857258938550699575L;
    private final String interfaceName;

    public HostInterfaceRemove(String name) {
        this.interfaceName = name;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(HostModel element) throws UpdateFailedException {
        if(element.removeInterface(interfaceName)) {
            throw new UpdateFailedException(String.format("network interface (%s) not found", interfaceName));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbstractHostModelUpdate<?> getCompensatingUpdate(HostModel original) {
        final InterfaceElement element = original.getInterface(interfaceName);
        if(element == null) {
            return null;
        }
        return new HostInterfaceAdd(new InterfaceAdd(element));
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerModelInterfaceRemove(interfaceName);
    }

}
