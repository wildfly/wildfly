/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.client.descriptor.passbyvalue;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Joshua Swett
 *
 */
@Stateless
@Remote(TestEJBRemote.class)
public class TestEJB implements TestEJBRemote {

    @Override
    public String getObjectReference(DummySerializableObject obj) {
        return obj.toString();
    }
}
