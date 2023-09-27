/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

/**
 * Exception indicating that a component receiving a method call has stopped.
 *
 * @author Richard Achmatowicz
 */
public class ComponentIsStoppedException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>ComponentIsStoppedException</code> with no detail message.
     */
    public ComponentIsStoppedException() {
    }

    /**
     * Constructs a <code>ComponentIsStoppedException</code> with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public ComponentIsStoppedException(String message) {
        super(message);
    }
}
