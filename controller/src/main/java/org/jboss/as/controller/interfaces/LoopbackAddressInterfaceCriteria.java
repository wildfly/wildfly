/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.ControllerLogger.SERVER_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.jboss.dmr.ModelNode;

/**
 * A loopback criteria with a specified bind address.
 *
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @version $Revision:$
 */
public class LoopbackAddressInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = 1L;

    private ModelNode address;
    private InetAddress resolved;
    private boolean unknownHostLogged;

    /**
     * Creates a new LoopbackAddressInterfaceCriteria
     *
     * @param address a valid value to pass to {@link InetAddress#getByName(String)}
     *                Cannot be {@code null}
     *
     * @throws IllegalArgumentException if <code>network</code> is <code>null</code>
     */
    public LoopbackAddressInterfaceCriteria(final InetAddress address) {
        if (address == null)
            throw MESSAGES.nullVar("address");
        this.resolved = address;
        this.address = new ModelNode(resolved.getHostAddress());
    }

    /**
     * Creates a new LoopbackAddressInterfaceCriteria
     *
     * @param address a valid value to pass to {@link InetAddress#getByName(String)}
     *                Cannot be {@code null}
     *
     * @throws IllegalArgumentException if <code>network</code> is <code>null</code>
     */
    public LoopbackAddressInterfaceCriteria(final ModelNode address) {
        if (address == null)
            throw MESSAGES.nullVar("address");
        this.address = address;
    }

    public synchronized InetAddress getAddress() throws UnknownHostException {
        if (resolved == null) {
            resolved = InetAddress.getByName(address.resolve().asString());
        }
        return this.resolved;
    }

        /**
     * {@inheritDoc}
     *
     * @return <code>{@link #getAddress()}()</code> if {@link NetworkInterface#isLoopback()} is true, null otherwise.
     */
    @Override
    public InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        try {
            if( networkInterface.isLoopback() ) {
                InetAddress resolved = getAddress();
                return resolved;
            }
        } catch (UnknownHostException e) {
            // One time only log a warning
            if (!unknownHostLogged) {
                SERVER_LOGGER.cannotResolveAddress(this.address);
                unknownHostLogged = true;
            }
        }
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("LoopbackAddressInterfaceCriteria(");
        sb.append("address=");
        sb.append(address);
        sb.append(",resolved=");
        sb.append(resolved);
        sb.append(')');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LoopbackAddressInterfaceCriteria == false) {
            return false;
        }
        return address.equals(((LoopbackAddressInterfaceCriteria)o).address);
    }
}
