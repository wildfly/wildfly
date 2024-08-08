/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.service.ServiceNameProvider;

/**
 * @author Paul Ferraro
 */
public interface PrimaryProxyContext<T> extends ServiceNameProvider {
    CommandDispatcher<GroupMember, LegacySingletonContext<T>> getCommandDispatcher();
}
