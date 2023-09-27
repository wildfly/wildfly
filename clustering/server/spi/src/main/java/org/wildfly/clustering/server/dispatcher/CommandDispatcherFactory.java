/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.dispatcher;

import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public interface CommandDispatcherFactory extends org.wildfly.clustering.dispatcher.CommandDispatcherFactory {

    @Override
    default <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context) {
        return this.createCommandDispatcher(id, context, WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context, ClassLoader loader);
}
