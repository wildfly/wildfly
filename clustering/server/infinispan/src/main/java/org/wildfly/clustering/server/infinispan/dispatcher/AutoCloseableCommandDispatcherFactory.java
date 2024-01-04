/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.dispatcher;

import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;

/**
 * A command dispatcher factory with a specific lifecycle (i.e. that must be closed).
 * @author Paul Ferraro
 */
public interface AutoCloseableCommandDispatcherFactory extends CommandDispatcherFactory, AutoCloseable {
    @Override
    void close();
}
