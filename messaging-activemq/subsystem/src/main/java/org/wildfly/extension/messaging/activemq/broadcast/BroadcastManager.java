/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.concurrent.TimeUnit;

/**
 * @author Paul Ferraro
 */
public interface BroadcastManager extends BroadcastReceiver {

    byte[] getBroadcast() throws InterruptedException;

    byte[] getBroadcast(long timeout, TimeUnit unit) throws InterruptedException;

    void clear();
}
