/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;

/**
 * A {@link CommandDispatcherFactory} that is also a registry of {@link BroadcastReceiver}s.
 * @author Paul Ferraro
 */
public interface BroadcastCommandDispatcherFactory extends CommandDispatcherFactory<GroupMember>, BroadcastReceiverRegistrar {

}
