/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Supplier;

import org.wildfly.clustering.group.Group;

/**
 * Context for local singleton services.
 * @author Paul Ferraro
 */
public interface LocalSingletonServiceContext extends SingletonServiceContext {
    Supplier<Group> getGroup();
}
