/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.byreference;


import jakarta.ejb.Stateless;

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
