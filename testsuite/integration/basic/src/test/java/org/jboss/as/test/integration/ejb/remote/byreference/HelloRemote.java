/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.byreference;

import jakarta.ejb.Remote;

@Remote
public interface HelloRemote {
    public TransferReturnValue hello ( TransferParameter param ) throws RemoteByReferenceException;

    public SerializableObject helloSerializable ( SerializableObject param ) throws RemoteByReferenceException;

    public NonSerializableObject helloNonSerializable(NonSerializableObject param) throws RemoteByReferenceException;

    public SerializableObject helloNonSerializableToSerializable(NonSerializableObject param) throws RemoteByReferenceException;

    public NonSerializableObject helloSerializableToNonSerializable(SerializableObject param) throws RemoteByReferenceException;
}

