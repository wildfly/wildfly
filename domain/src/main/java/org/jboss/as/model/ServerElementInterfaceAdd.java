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
 * Update adding a {@link InterfaceElement} to the {@link ServerElement}.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerElementInterfaceAdd extends AbstractModelElementUpdate<ServerElement> {

    private static final long serialVersionUID = -8245273058656370269L;
    private final InterfaceAdd delegate;

    public ServerElementInterfaceAdd(InterfaceAdd delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(final ServerElement server) throws UpdateFailedException {
        final InterfaceElement networkInterface = server.addInterface(delegate.getName());
        if(networkInterface == null) {
            throw new UpdateFailedException();
        }
        delegate.applyUpdate(networkInterface);
    }

    /** {@inheritDoc} */
    public AbstractModelElementUpdate<ServerElement> getCompensatingUpdate(final ServerElement server) {
        return new ServerElementInterfaceRemove(delegate.getName());
    }

    /** {@inheritDoc} */
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerModelInterfaceAdd(delegate);
    }

    /** {@inheritDoc} */
    public Class<ServerElement> getModelElementType() {
        return ServerElement.class;
    }

}
