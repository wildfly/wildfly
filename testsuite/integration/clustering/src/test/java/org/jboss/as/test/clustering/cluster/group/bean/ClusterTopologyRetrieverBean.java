package org.jboss.as.test.clustering.cluster.group.bean;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;

@Stateless
@Remote(ClusterTopologyRetriever.class)
public class ClusterTopologyRetrieverBean implements ClusterTopologyRetriever {
    @EJB
    private Group group;

    @Override
    public ClusterTopology getClusterTopology() {
        return new ClusterTopology(this.group.getLocalMember().getName(), getNames(this.group.getMembership()), getNames(this.group.getPreviousMembership()));
    }

    private static List<String> getNames(Membership membership) {
        return (membership != null) ? membership.getMembers().stream().map(Node::getName).collect(Collectors.toList()) : Collections.emptyList();
    }
}
