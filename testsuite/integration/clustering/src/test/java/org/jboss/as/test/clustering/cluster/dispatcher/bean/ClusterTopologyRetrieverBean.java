package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
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

            return new ClusterTopology(nodes, local, remote);
        } catch (CommandDispatcherException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
