/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.client.descriptor.passbyvalue;

/**
 * @author Joshua Swett
 *
 */
public interface TestEJBRemote {

    String getObjectReference(DummySerializableObject obj);

}
