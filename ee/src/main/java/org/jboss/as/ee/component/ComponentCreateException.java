/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

/**
 * An exception relating to a problem with the creation of an EE component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ComponentCreateException extends Exception {

    private static final long serialVersionUID = 4525122726559539936L;

    /**
     * Constructs a {@code ComponentCreateException} with no detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public ComponentCreateException() {
        super((String) null);
    }

    /**
     * Constructs a {@code ComponentCreateException} with the specified detail message. The cause is not initialized,
     * and may subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public ComponentCreateException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a {@code ComponentCreateException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ComponentCreateException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@code ComponentCreateException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ComponentCreateException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
