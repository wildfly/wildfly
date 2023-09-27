/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;

import java.io.Serializable;

/**
 * A simple immediately-available {@link ManagedReference}'s instance.
 *
 * @author Eduardo Martins
 *
 */
public class ImmediateManagedReference implements ManagedReference, Serializable {

    private final Object instance;

    public ImmediateManagedReference(final Object instance) {
        this.instance = instance;
    }

    @Override
    public void release() {

    }

    @Override
    public Object getInstance() {
        return instance;
    }
}
