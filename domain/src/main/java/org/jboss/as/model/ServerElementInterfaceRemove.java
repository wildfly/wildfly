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
 * Update removing a {@link InterfaceElement} from the {@link ServerElement}.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerElementInterfaceRemove extends AbstractModelUpdate<ServerElement, Void> {

    private static final long serialVersionUID = -2830177164001085749L;
    private final String interfaceName;

    public ServerElementInterfaceRemove(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final ServerElement server) throws UpdateFailedException {
        if(! server.removeInterface(interfaceName)) {
            throw new UpdateFailedException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public ServerElementInterfaceAdd getCompensatingUpdate(ServerElement original) {
        final InterfaceElement networkInterface = original.getInterface(interfaceName);
        if(networkInterface == null) {
            return null;
        }
        return new ServerElementInterfaceAdd(new InterfaceAdd(networkInterface));
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerModelInterfaceRemove(interfaceName);
    }

    /** {@inheritDoc} */
    @Override
    public Class<ServerElement> getModelElementType() {
        return ServerElement.class;
    }
}
