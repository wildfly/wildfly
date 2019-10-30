package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import java.io.Serializable;
import java.util.Collection;

public class ClusterTopology implements Serializable {
    private static final long serialVersionUID = 413628123168918069L;

    private final Collection<String> nodes;
    private final String localNode;
    private final Collection<String> remoteNodes;

    public ClusterTopology(Collection<String> nodes, String localNode, Collection<String> remoteNodes) {
        this.nodes = nodes;
        this.localNode = localNode;
        this.remoteNodes = remoteNodes;
    }

    public Collection<String> getNodes() {
        return this.nodes;
    }

    public String getLocalNode() {
        return this.localNode;
    }

    public Collection<String> getRemoteNodes() {
        return this.remoteNodes;
    }
}
