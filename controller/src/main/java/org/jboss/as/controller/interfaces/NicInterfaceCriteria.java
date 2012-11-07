/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

/**
 *
 */
package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given name matches the
 * network interface's {@link NetworkInterface#getName() name}.
 *
 * @author Brian Stansberry
 */
public class NicInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = 6905500001319165842L;

    private final String name;

    /**
     * Creates a new AnyInterfaceCriteria
     *
     * @param name the criteria to check to see if any are satisfied.
     *                 Cannot be <code>null</code>
     *
     * @throws IllegalArgumentException if <code>criteria</code> is <code>null</code>
     */
    public NicInterfaceCriteria(String name) {
        if (name == null)
            throw MESSAGES.nullVar("name");
        this.name = name;
    }

    public String getAcceptableName() {
        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if the {@link #getAcceptableName() acceptable name}
     *          equals <code>networkInterface</code>'s {@link NetworkInterface#getName() name}.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( name.equals(networkInterface.getName()) )
            return address;
        return null;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof NicInterfaceCriteria)
                && name.equals(((NicInterfaceCriteria)o).name);
    }

}
