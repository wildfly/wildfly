/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.dispatcher;

/**
 * Indicates a failure to dispatch a command.
 * @author Paul Ferraro
 * @deprecated To be removed without replacement.
 */
@Deprecated(forRemoval = true)
public class CommandDispatcherException extends Exception {
    private static final long serialVersionUID = 3984965224844057380L;

    /**
     * Creates a new CommandDispatcherException using the specified cause.
     * @param cause the cause of this exception.
     */
    public CommandDispatcherException(Throwable cause) {
        super(cause);
    }
}
