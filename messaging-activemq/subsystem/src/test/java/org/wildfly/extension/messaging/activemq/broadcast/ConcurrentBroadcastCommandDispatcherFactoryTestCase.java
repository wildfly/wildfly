/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;

/**
 * @author Paul Ferraro
 */
public class ConcurrentBroadcastCommandDispatcherFactoryTestCase {

    private final CommandDispatcherFactory factory = mock(CommandDispatcherFactory.class);

    @Test
    public void registration() {
        BroadcastReceiver receiver1 = mock(BroadcastReceiver.class);
        BroadcastReceiver receiver2 = mock(BroadcastReceiver.class);
        BroadcastReceiverRegistrar registrar = new ConcurrentBroadcastCommandDispatcherFactory(this.factory);
        byte[] data1 = new byte[] { 1 };
        byte[] data2 = new byte[] { 2 };
        byte[] data3 = new byte[] { 3 };

        try (Registration registration1 = registrar.register(receiver1)) {
            try (Registration registration2 = registrar.register(receiver2)) {
                registrar.receive(data1);

                verify(receiver1).receive(data1);
                verify(receiver2).receive(data1);
            }

            registrar.receive(data2);

            verify(receiver1).receive(data2);
            verifyNoMoreInteractions(receiver2);
        }

        registrar.receive(data3);

        verifyNoMoreInteractions(receiver1);
        verifyNoMoreInteractions(receiver2);
    }

    @Test
    public void getGroup() {
        Group group = mock(Group.class);
        CommandDispatcherFactory factory = new ConcurrentBroadcastCommandDispatcherFactory(this.factory);

        when(this.factory.getGroup()).thenReturn(group);

        Group result = factory.getGroup();

        Assert.assertSame(group, result);
    }

    @Test
    public void createCommandDispatcher() {
        CommandDispatcher<Object> dispatcher = mock(CommandDispatcher.class);
        Object id = new Object();
        Object context = new Object();
        CommandDispatcherFactory factory = new ConcurrentBroadcastCommandDispatcherFactory(this.factory);

        when(this.factory.createCommandDispatcher(same(id), any(), any())).thenReturn(dispatcher);
        when(dispatcher.getContext()).thenReturn(context);

        // Verify that dispatcher does not close until all created dispatchers are closed
        try (CommandDispatcher<Object> dispatcher1 = factory.createCommandDispatcher(id, new Object())) {
            Assert.assertSame(context, dispatcher1.getContext());

            try (CommandDispatcher<Object> dispatcher2 = factory.createCommandDispatcher(id, new Object())) {
                Assert.assertSame(context, dispatcher2.getContext());
            }

            verify(dispatcher, never()).close();
        }

        verify(dispatcher).close();
    }
}
