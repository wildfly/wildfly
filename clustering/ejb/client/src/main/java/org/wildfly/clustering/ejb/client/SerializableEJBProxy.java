/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.client;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBLocator;

/**
 * Serializes an EJB proxy via its serializable locator.
 * @author Paul Ferraro
 */
public class SerializableEJBProxy implements Serializable {
    private static final long serialVersionUID = 2301976566323836449L;

    private final EJBLocator<? extends Object> locator;

    SerializableEJBProxy(Object proxy) {
        this.locator = EJBClient.getLocatorFor(proxy);
    }

    @SuppressWarnings("unused")
    private Object readResolve() throws ObjectStreamException {
        return EJBClient.createProxy(this.locator);
    }
}