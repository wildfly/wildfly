/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.dispatcher.bean.legacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetriever;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Node;

@Stateless
@Remote(ClusterTopologyRetriever.class)
public class LegacyClusterTopologyRetrieverBean implements ClusterTopologyRetriever {
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
