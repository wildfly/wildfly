/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.dispatcher;

import org.wildfly.clustering.group.Group;

/**
 * Factory for creating a command dispatcher.
 *
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory}.
 */
@Deprecated(forRemoval = true)
public interface CommandDispatcherFactory {

    /**
     * Returns the group upon which the this command dispatcher operates.
     *
     * @return a group
     */
    Group getGroup();

    /**
     * Creates a new command dispatcher using the specified identifier and context.
     * The resulting {@link CommandDispatcher} will communicate with those dispatchers within the group sharing the same identifier.
     * @param id      a unique identifier for this dispatcher
     * @param context the context used for executing commands
     * @return a new command dispatcher
     */
    <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context);

    /**
     * Creates a new command dispatcher using the specified identifier and context, whose marshaller is configured via the specified class loader.
     * The resulting {@link CommandDispatcher} will communicate with those dispatchers within the group sharing the same identifier.
     * @param id      a unique identifier for this dispatcher
     * @param context the context used for executing commands
     * @return a new command dispatcher
     */
    default <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context, ClassLoader loader) {
        return this.createCommandDispatcher(id, context);
    }
}
