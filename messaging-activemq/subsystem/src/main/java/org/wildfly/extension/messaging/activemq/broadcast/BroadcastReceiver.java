/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

/**
 * A receiver of a broadcast.
 * @author Paul Ferraro
 */
public interface BroadcastReceiver {
    /**
     * Receives the specified broadcast data.
     * @param data broadcast data
     */
    void receive(byte[] data);
}
