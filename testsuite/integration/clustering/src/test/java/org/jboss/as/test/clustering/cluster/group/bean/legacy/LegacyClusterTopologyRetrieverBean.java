/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.group.bean.legacy;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.jboss.as.test.clustering.cluster.group.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.group.bean.ClusterTopologyRetriever;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;

@Stateless
@Remote(ClusterTopologyRetriever.class)
public class LegacyClusterTopologyRetrieverBean implements ClusterTopologyRetriever {
    @EJB
    private LegacyGroup group;

    @Override
    public ClusterTopology getClusterTopology() {
        return new ClusterTopology(this.group.getLocalMember().getName(), getNames(this.group.getMembership()), getNames(this.group.getPreviousMembership()));
    }

    private static List<String> getNames(Membership membership) {
        return (membership != null) ? membership.getMembers().stream().map(Node::getName).collect(Collectors.toList()) : Collections.emptyList();
    }
}
