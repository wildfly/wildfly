/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;

/**
 * Creates container managed references.
 * <p/>
 * Usages of these references is very much implementation specific, however implementers should be
 * careful to make sure that the references are idempotent.
 *
 * @author Stuart Douglas
 */
public interface ManagedReferenceFactory {

    /**
     * Get a new managed instance reference.
     *
     * @return a reference to a managed object
     */
    ManagedReference getReference();
}
