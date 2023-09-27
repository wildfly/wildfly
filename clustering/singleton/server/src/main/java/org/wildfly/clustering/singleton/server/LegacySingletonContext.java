/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.server;

import java.util.Optional;

/**
 * Context for singleton commands.
 * @author Paul Ferraro
 */
public interface LegacySingletonContext<T> extends SingletonContext {

    Optional<T> getLocalValue();
}
