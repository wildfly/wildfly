/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.group.bean;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.GroupMembership;

@Stateless
@Remote(ClusterTopologyRetriever.class)
public class ClusterTopologyRetrieverBean implements ClusterTopologyRetriever {
    @EJB
    private Group group;

    @Override
    public ClusterTopology getClusterTopology() {
        return new ClusterTopology(this.group.getLocalMember().getName(), getNames(this.group.getMembership()), getNames(this.group.getPreviousMembership()));
    }

    private static List<String> getNames(GroupMembership<GroupMember> membership) {
        return (membership != null) ? membership.getMembers().stream().map(GroupMember::getName).collect(Collectors.toList()) : Collections.emptyList();
    }
}
