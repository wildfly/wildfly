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

package org.wildfly.clustering.server.dispatcher;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.junit.Test;

/**
 * Unit test for {@link ManagedCommandDispatcher}.
 * @author Paul Ferraro
 */
public class ManagedCommandDispatcherFactoryTestCase {
    @Test
    public void test() {
        AutoCloseableCommandDispatcherFactory factory = mock(AutoCloseableCommandDispatcherFactory.class);

        try (AutoCloseableCommandDispatcherFactory subject = new ManagedCommandDispatcherFactory(factory)) {
            String context = "context";
            CommandDispatcher<String> dispatcher = mock(CommandDispatcher.class);

            when(factory.createCommandDispatcher("foo", context)).thenReturn(dispatcher);
            when(dispatcher.getContext()).thenReturn(context);

            try (CommandDispatcher<String> dispatcher1 = subject.createCommandDispatcher("foo", context)) {

                assertSame(context, dispatcher1.getContext());

                try (CommandDispatcher<String> dispatcher2 = subject.createCommandDispatcher("foo", context)) {

                    assertSame(dispatcher1, dispatcher2);

                    String otherContext = "unexpected";

                    try {
                        subject.createCommandDispatcher("foo", otherContext);
                        fail();
                    } catch (IllegalArgumentException e) {
                        verify(factory, never()).createCommandDispatcher("foo", otherContext);
                    }
                }

                verify(dispatcher, never()).close();
            }

            verify(dispatcher).close();
        }

        verify(factory).close();
    }
}
