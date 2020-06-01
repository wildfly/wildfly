/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.remote.byreference;


import javax.ejb.Stateless;

import org.jboss.logging.Logger;

@Stateless
public class HelloBean implements HelloRemote {

    private final Logger log = Logger.getLogger(this.getClass());

    public TransferReturnValue hello ( TransferParameter param ) throws RemoteByReferenceException {
        log.debug("hello("+ param +") = Hello " + param );

        if(param == null)
            throw new RemoteByReferenceException("Param was null");

        return new TransferReturnValue ( "Hello " + param );
    }

    @Override
    public SerializableObject helloSerializable(SerializableObject param) throws RemoteByReferenceException {
        log.debug("helloserializable("+ param +") = Hello " + param );

        if(param == null)
            throw new RemoteByReferenceException("Param was null");
        param.setValue("Bye");

        return param;
    }

    @Override
    public NonSerializableObject helloNonSerializable(NonSerializableObject param) throws RemoteByReferenceException {
        log.debug("helloserializable("+ param +") = Hello " + param );

        if(param == null)
            throw new RemoteByReferenceException("Param was null");
        param.setValue("Bye");

        return param;
    }

    @Override
    public SerializableObject helloNonSerializableToSerializable(NonSerializableObject param)
            throws RemoteByReferenceException {
        log.debug("helloserializable("+ param +") = Hello " + param );

        if(param == null)
            throw new RemoteByReferenceException("Param was null");
        param.setValue("Bye");

        return new SerializableObject(param.getValue());
    }

    @Override
    public NonSerializableObject helloSerializableToNonSerializable(SerializableObject param)
            throws RemoteByReferenceException {
        log.debug("helloserializable("+ param +") = Hello " + param );

        if(param == null)
            throw new RemoteByReferenceException("Param was null");
        param.setValue("Bye");

        return new NonSerializableObject(param.getValue());
    }
}