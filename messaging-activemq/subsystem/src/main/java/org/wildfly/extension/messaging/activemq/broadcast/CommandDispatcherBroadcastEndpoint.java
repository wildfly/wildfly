/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.activemq.artemis.api.core.BroadcastEndpoint;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * A {@link BroadcastEndpoint} based on a {@link CommandDispatcher}.
 * @author Paul Ferraro
 */
public class CommandDispatcherBroadcastEndpoint implements BroadcastEndpoint {

    private enum Mode {
        BROADCASTER, RECEIVER, CLOSED;
    }

    private final CommandDispatcherFactory<GroupMember> factory;
    private final String name;
    private final BroadcastReceiverRegistrar registrar;
    private final Function<String, BroadcastManager> managerFactory;
    private final AtomicReference<Mode> mode = new AtomicReference<>(Mode.CLOSED);

    private volatile BroadcastManager manager = null;
    private volatile Registration registration = null;
    private volatile CommandDispatcher<GroupMember, BroadcastReceiver> dispatcher;

    public CommandDispatcherBroadcastEndpoint(CommandDispatcherFactory<GroupMember> factory, String name, BroadcastReceiverRegistrar registrar, Function<String, BroadcastManager> managerFactory) {
        this.factory = factory;
        this.name = name;
        this.registrar = registrar;
        this.managerFactory = managerFactory;
    }

    @Override
    public void openClient() throws Exception {
        if (this.mode.compareAndSet(Mode.CLOSED, Mode.RECEIVER)) {
            this.manager = this.managerFactory.apply(this.name);
            this.registration = this.registrar.register(this.manager);
            this.open();
        }
    }

    @Override
    public void openBroadcaster() throws Exception {
        if (this.mode.compareAndSet(Mode.CLOSED, Mode.BROADCASTER)) {
            this.open();
        }
    }

    private void open() {
        this.dispatcher = this.factory.createCommandDispatcher(this.name, this.registrar);
    }

    @Override
    public void close(boolean isBroadcast) throws Exception {
        if (this.mode.getAndSet(Mode.CLOSED) != Mode.CLOSED) {
            if (this.dispatcher != null) {
                this.dispatcher.close();
            }
            if (this.registration != null) {
                this.registration.close();
            }
            if (this.manager != null) {
                this.manager.clear();
            }
        }
    }

    @Override
    public void broadcast(byte[] data) throws Exception {
        if (this.mode.get() == Mode.BROADCASTER) {
            if (MessagingLogger.ROOT_LOGGER.isDebugEnabled()) {
                MessagingLogger.ROOT_LOGGER.debugf("Broadcasting to group %s: %s", this.name, Arrays.toString(data));
            }
            this.dispatcher.dispatchToGroup(new BroadcastCommand(data));
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
