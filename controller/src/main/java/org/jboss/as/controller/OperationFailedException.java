/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * Exception indicating an operation has failed due to a client mistake (e.g. an operation with
 * invalid parameters was invoked.) Should not be used to report server failures.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class OperationFailedException extends Exception implements OperationClientException {

    private final ModelNode failureDescription;

    private static final long serialVersionUID = -1896884563520054972L;

    /**
     * Constructs a {@code OperationFailedException} with the given message.
     * The message is also used as the {@link #getFailureDescription() failure description}.
     * The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param message the description of the failure
     */
    public OperationFailedException(final String message) {
        this(message, new ModelNode(message));
    }

    /**
     * Constructs a {@code OperationFailedException} with the specified cause and message.
     * The message is also used as the {@link #getFailureDescription() failure description}.
     *
     * @param message the description of the failure
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public OperationFailedException(final String message, final Throwable cause) {
        this(message, cause, new ModelNode(message));
    }

    /**
     * Constructs a {@code OperationFailedException} with no detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param description the description of the failure
     */
    public OperationFailedException(final ModelNode description) {
        failureDescription = description;
    }

    /**
     * Constructs a {@code OperationFailedException} with the specified detail message. The cause is not initialized,
     * and may subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     * @param description the description of the failure
     */
    public OperationFailedException(final String msg, final ModelNode description) {
        super(msg);
        failureDescription = description;
    }

    /**
     * Constructs a {@code OperationFailedException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     * @param description the description of the failure
     */
    public OperationFailedException(final Throwable cause, final ModelNode description) {
        super(cause);
        failureDescription = description;
    }

    /**
     * Constructs a {@code OperationFailedException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     * @param description the description of the failure
     */
    public OperationFailedException(final String msg, final Throwable cause, final ModelNode description) {
        super(msg, cause);
        failureDescription = description;
    }

    /**
     * Get the detyped failure description.
     *
     * @return the description
     */
    public ModelNode getFailureDescription() {
        return failureDescription;
    }

    @Override
    public String toString() {
        return super.toString() + " [ " + failureDescription + " ]";
    }
}
