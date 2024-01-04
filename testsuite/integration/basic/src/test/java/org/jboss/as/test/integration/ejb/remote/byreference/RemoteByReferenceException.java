/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.byreference;

public class RemoteByReferenceException extends Exception {

    private NonSerializableObject nonSerializableObject;

    public RemoteByReferenceException() {
        super();
        nonSerializableObject = new NonSerializableObject("null");
    }

    public RemoteByReferenceException(String msg) {
        super(msg);
        nonSerializableObject = new NonSerializableObject(msg);
    }
}

