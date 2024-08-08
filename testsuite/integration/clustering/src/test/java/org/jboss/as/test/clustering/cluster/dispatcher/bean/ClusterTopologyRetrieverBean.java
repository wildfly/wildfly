/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.Command;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;

@Stateless
@Remote(ClusterTopologyRetriever.class)
public class ClusterTopologyRetrieverBean implements ClusterTopologyRetriever {
    @EJB
    private CommandDispatcher<GroupMember, GroupMember> dispatcher;
    @EJB
    private CommandDispatcherFactory<GroupMember> factory;
    private final Command<String, GroupMember, RuntimeException> command = new TestCommand();
    private final Command<Void, GroupMember, Exception> exceptionCommand = new ExceptionCommand();

    @Override
    public ClusterTopology getClusterTopology() {
        try {
            Collection<CompletionStage<String>> responses = this.dispatcher.dispatchToGroup(this.command).values();
            List<String> nodes = new ArrayList<>(responses.size());
            for (CompletionStage<String> response: responses) {
                try {
                    nodes.add(response.toCompletableFuture().join());
                } catch (CancellationException e) {
                    // Ignore
                }
            }

            GroupMember localMember = this.factory.getGroup().getLocalMember();
            String local = this.dispatcher.dispatchToMember(this.command, localMember).toCompletableFuture().join();

            responses = this.dispatcher.dispatchToGroup(this.command, Set.of(localMember)).values();
            List<String> remote = new ArrayList<>(responses.size());
            for (CompletionStage<String> response: responses) {
                try {
                    remote.add(response.toCompletableFuture().join());
                } catch (CancellationException e) {
                    // Ignore
                }
            }

            for (CompletionStage<Void> response: this.dispatcher.dispatchToGroup(this.exceptionCommand).values()) {
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
        } catch (IOException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
