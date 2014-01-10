package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.CommandResponse;
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
            System.out.println("Executing command on cluster");
            Collection<CommandResponse<String>> responses = this.dispatcher.executeOnCluster(this.command).values();
            List<String> nodes = new ArrayList<>(responses.size());
            for (CommandResponse<String> response: responses) {
                nodes.add(response.get());
            }

            Node localNode = this.factory.getGroup().getLocalNode();
            System.out.println("Executing command on node: " + localNode);
            String local = this.dispatcher.executeOnNode(this.command, localNode).get();

            System.out.println("Executing command on cluster excluding node: " + localNode);
            responses = this.dispatcher.executeOnCluster(this.command, localNode).values();
            List<String> remote = new ArrayList<>(responses.size());
            for (CommandResponse<String> response: responses) {
                remote.add(response.get());
            }

            return new ClusterTopology(nodes, local, remote);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
