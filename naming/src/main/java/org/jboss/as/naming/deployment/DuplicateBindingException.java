/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.deployment;

/**
 * Exception thrown if a deployment contains duplicate non-compatible JNDI bindings.
 *
 * @author John E. Bailey
 */
public class DuplicateBindingException extends Exception {
    private static final long serialVersionUID = 131033218044790395L;

    public DuplicateBindingException(final String message) {
        super(message);
    }
}
