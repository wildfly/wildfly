/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;

/**
 * A reference to a container managed object
 *
 * @author Stuart Douglas
 */
public interface ManagedReference {
    /**
     * Release the reference. Depending on the implementation this may destroy
     * the underlying object.
     * <p/>
     * Implementers should take care to make this method idempotent,
     * as it may be called multiple times.
     */
    void release();

    /**
     * Get the object instance.
     *
     * @return the object this reference refers to
     */
    Object getInstance();
}
