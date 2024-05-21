/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.io.Serializable;

import org.wildfly.clustering.server.dispatcher.Command;

/**
 * A {@link Command} that receives a broadcast.
 * @author Paul Ferraro
 */
public class BroadcastCommand implements Command<Void, BroadcastReceiver, RuntimeException>, Serializable {
    private static final long serialVersionUID = 4354035602902924182L;

    private final byte[] data;

    public BroadcastCommand(byte[] data) {
        this.data = data;
    }

    @Override
    public Void execute(BroadcastReceiver receiver) {
        receiver.receive(this.data);
        return null;
    }
}
