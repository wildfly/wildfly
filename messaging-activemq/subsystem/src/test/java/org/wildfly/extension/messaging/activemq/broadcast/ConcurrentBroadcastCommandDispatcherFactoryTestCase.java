/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import static org.mockito.Mockito.*;

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

    private final org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory factory = mock(org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory.class);

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
