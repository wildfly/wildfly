/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import org.wildfly.clustering.server.Registrar;

/**
 * A {@link BroadcastReceiver} that notifies a set of registered {@link BroadcastReceiver} instances.
 * @author Paul Ferraro
 */
public interface BroadcastReceiverRegistrar extends BroadcastReceiver, Registrar<BroadcastReceiver> {
}
