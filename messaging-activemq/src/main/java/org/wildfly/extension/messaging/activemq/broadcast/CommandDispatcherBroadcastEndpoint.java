/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.activemq.artemis.api.core.BroadcastEndpoint;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;

/**
 * A {@link BroadcastEndpoint} based on a {@link CommandDispatcher}.
 * @author Paul Ferraro
 */
public class CommandDispatcherBroadcastEndpoint implements BroadcastEndpoint {

    private enum Mode {
        BROADCASTER, RECEIVER, CLOSED;
    }

    private final CommandDispatcherFactory factory;
    private final String name;
    private final BroadcastManager manager;
    private final AtomicReference<Mode> mode = new AtomicReference<>(Mode.CLOSED);

    private volatile CommandDispatcher<BroadcastReceiver> dispatcher;

    public CommandDispatcherBroadcastEndpoint(CommandDispatcherFactory factory, String name, BroadcastManager manager) {
        this.factory = factory;
        this.name = name;
        this.manager = manager;
    }

    @Override
    public void openClient() throws Exception {
        if (this.mode.compareAndSet(Mode.CLOSED, Mode.RECEIVER)) {
            this.open();
        }
    }

    @Override
    public void openBroadcaster() throws Exception {
        if (this.mode.compareAndSet(Mode.CLOSED, Mode.BROADCASTER)) {
            this.open();
        }
    }

    private void open() throws Exception {
        this.dispatcher = this.factory.createCommandDispatcher(this.name, this.manager);
    }

    @Override
    public void close(boolean isBroadcast) throws Exception {
        if (this.mode.getAndSet(Mode.CLOSED) != Mode.CLOSED) {
            this.dispatcher.close();
            this.manager.clear();
        }
    }

    @Override
    public void broadcast(byte[] data) throws Exception {
        if (this.mode.get() == Mode.BROADCASTER) {
            this.dispatcher.executeOnGroup(new BroadcastCommand(data));
        }
    }

    @Override
    public byte[] receiveBroadcast() throws Exception {
        return (this.mode.get() == Mode.RECEIVER) ? this.manager.getBroadcast() : null;
    }

    @Override
    public byte[] receiveBroadcast(long time, TimeUnit unit) throws Exception {
        return (this.mode.get() == Mode.RECEIVER) ? this.manager.getBroadcast(time, unit) : null;
    }
}
