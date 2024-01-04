/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.dispatcher;

import java.io.Serializable;

/**
 * A command to invoke remotely.
 *
 * @param <C> the command context type
 * @param <R> the command return type
 * @author Paul Ferraro
 */
public interface Command<R, C> extends Serializable {

    /**
     * Execute this command with the specified context.
     *
     * @param context the execution context
     * @return the result of this command
     * @throws Exception exception that occurred during execution
     */
    R execute(C context) throws Exception;
}
