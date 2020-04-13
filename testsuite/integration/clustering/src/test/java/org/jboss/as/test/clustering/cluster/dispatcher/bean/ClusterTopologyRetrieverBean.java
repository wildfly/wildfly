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
package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Node;

@Stateless
@Remote(ClusterTopologyRetriever.class)
public class ClusterTopologyRetrieverBean implements ClusterTopologyRetriever {
    @EJB
    private CommandDispatcher<Node> dispatcher;
    @EJB
    private CommandDispatcherFactory factory;
    private final Command<String, Node> command = new TestCommand();
    private final Command<Void, Node> exceptionCommand = new ExceptionCommand();

    @Override
    public ClusterTopology getClusterTopology() {
        try {
            Collection<CompletionStage<String>> responses = this.dispatcher.executeOnGroup(this.command).values();
            List<String> nodes = new ArrayList<>(responses.size());
            for (CompletionStage<String> response: responses) {
                try {
                    nodes.add(response.toCompletableFuture().join());
                } catch (CancellationException e) {
                    // Ignore
                }
            }

            Node localNode = this.factory.getGroup().getLocalMember();
            String local = this.dispatcher.executeOnMember(this.command, localNode).toCompletableFuture().join();

            responses = this.dispatcher.executeOnGroup(this.command, localNode).values();
            List<String> remote = new ArrayList<>(responses.size());
            for (CompletionStage<String> response: responses) {
                try {
                    remote.add(response.toCompletableFuture().join());
                } catch (CancellationException e) {
                    // Ignore
                }
            }

            for (CompletionStage<Void> response: this.dispatcher.executeOnGroup(this.exceptionCommand).values()) {
                try {
                    response.toCompletableFuture().join();
                    throw new IllegalStateException("Exception expected");
                } catch (CancellationException e) {
                    // Ignore
                } catch (CompletionException e) {
                    e.printStackTrace(System.err);
                    assert Exception.class.equals(e.getCause().getClass()) : e.getCause().getClass().getName();
                }
            }

            return new ClusterTopology(nodes, local, remote);
        } catch (CommandDispatcherException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
