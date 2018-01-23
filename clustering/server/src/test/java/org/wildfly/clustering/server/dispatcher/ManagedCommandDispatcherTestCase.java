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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Test;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;

/**
 * Unit test for {@link ManagedCommandDispatcher}.
 * @author Paul Ferraro
 */
public class ManagedCommandDispatcherTestCase {

    @Test
    public void test() throws CommandDispatcherException {
        CommandDispatcher<String> dispatcher = mock(CommandDispatcher.class);
        Runnable closeTask = mock(Runnable.class);
        String context = "context";

        try (CommandDispatcher<String> subject = new ManagedCommandDispatcher<>(dispatcher, closeTask)) {

            when(dispatcher.getContext()).thenReturn(context);

            assertSame(context, subject.getContext());

            Command<Void, String> command = mock(Command.class);
            Node node = mock(Node.class);
            Node[] nodes = new Node[] { node };
            CommandResponse<Void> response = mock(CommandResponse.class);
            Future<Void> future = mock(Future.class);

            Map<Node, CommandResponse<Void>> responses = Collections.singletonMap(node, response);
            Map<Node, Future<Void>> futures = Collections.singletonMap(node, future);

            when(dispatcher.executeOnCluster(command, nodes)).thenReturn(responses);

            assertSame(responses, subject.executeOnCluster(command, nodes));

            when(dispatcher.executeOnNode(command, node)).thenReturn(response);

            assertSame(response, subject.executeOnNode(command, node));

            when(dispatcher.submitOnCluster(command, nodes)).thenReturn(futures);

            assertSame(futures, subject.submitOnCluster(command, nodes));

            when(dispatcher.submitOnNode(command, node)).thenReturn(future);

            assertSame(future, subject.submitOnNode(command, node));
        }

        verify(dispatcher, never()).close();
        verify(closeTask).run();
    }
}
