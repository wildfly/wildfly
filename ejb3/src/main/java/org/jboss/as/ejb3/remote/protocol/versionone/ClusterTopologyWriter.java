/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote.protocol.versionone;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.ejb.client.remoting.PackedInteger;

import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * A {@link ClusterTopologyWriter} is responsible for writing out cluster topology related EJB remoting protocol
 * messages to the {@link DataOutput} that's passed to its write methods
 *
 * @author Jaikiran Pai
 */
class ClusterTopologyWriter {

    private static final byte HEADER_COMPLETE_CLUSTER_TOPOLOGY = 0x15;
    private static final byte HEADER_CLUSTER_REMOVED = 0x16;
    private static final byte HEADER_NEW_NODES_ADDED = 0x17;
    private static final byte HEADER_NODES_REMOVED = 0x18;

    /**
     * Writes out a EJB remoting protocol message containing the cluster topology information for the passed <code>clusters</code>
     *
     * @param output   The {@link DataOutput} into which the message will be written
     * @param clusters The clusters whose topology will be written to the <code>output</code>
     * @throws IOException
     */
    void writeCompleteClusterTopology(final DataOutput output, final GroupMembershipNotifier... clusters) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null dataoutput");
        }
        if (clusters == null || clusters.length == 0) {
            return;
        }
        // write the header
        output.write(HEADER_COMPLETE_CLUSTER_TOPOLOGY);
        // write the cluster count
        PackedInteger.writePackedInteger(output, clusters.length);
        // write out each of the cluster's topology
        for (final GroupMembershipNotifier cluster : clusters) {
            // write the cluster name
            output.writeUTF(cluster.getGroupName());
            // write the member node count
            final int memberCount = cluster.getClusterNodes().size();
            PackedInteger.writePackedInteger(output, memberCount);
            // write out the member info for each member
            for (final ClusterNode clusterMember : cluster.getClusterNodes()) {
                // write the node name
                output.writeUTF(clusterMember.getName());
                // TODO: This should be configurable. Right now we just write out the textual form
                // of the IP address, but the configuration should allow sending back a hostname or an
                // IP address
                output.writeUTF(clusterMember.getIpAddress().getHostAddress());
                // TODO: We need a way to figure out EJB remoting port of a cluster member
                PackedInteger.writePackedInteger(output, 4447);
            }
        }

    }

    /**
     * Writes out a EJB remoting protocol message containing the names of the <code>clusters</code> which have been removed
     * from the server.
     *
     * @param output   The {@link DataOutput} to which the message will be written
     * @param clusters The clusters which have been removed
     * @throws IOException
     */
    void writeClusterRemoved(final DataOutput output, final GroupMembershipNotifier... clusters) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null dataoutput");
        }
        if (clusters == null || clusters.length == 0) {
            return;
        }
        // write the header
        output.write(HEADER_CLUSTER_REMOVED);
        // write the cluster count
        PackedInteger.writePackedInteger(output, clusters.length);
        // write out the cluster name for each of the removed cluster
        for (final GroupMembershipNotifier cluster : clusters) {
            output.writeUTF(cluster.getGroupName());
        }

    }

    void writeNewNodesAdded(final DataOutput output, final String clusterName, final List<ClusterNode> newNodes) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null dataoutput");
        }
        if (newNodes == null || newNodes.isEmpty()) {
            return;
        }
        // write the header
        output.write(HEADER_NEW_NODES_ADDED);
        // write the cluster count
        PackedInteger.writePackedInteger(output, 1);
        // write the cluster name
        output.writeUTF(clusterName);
        // write the new nodes added count
        final int newNodesCount = newNodes.size();
        PackedInteger.writePackedInteger(output, newNodesCount);
        // write out the member info for each new member
        for (final ClusterNode clusterMember : newNodes) {
            // write the node name
            output.writeUTF(clusterMember.getName());
            // TODO: This should be configurable. Right now we just write out the textual form
            // of the IP address, but the configuration should allow sending back a hostname or an
            // IP address
            output.writeUTF(clusterMember.getIpAddress().getHostAddress());
            // TODO: We need a way to figure out EJB remoting port of a cluster member
            PackedInteger.writePackedInteger(output, 4447);
        }
    }

    void writeNodesRemoved(final DataOutput output, final String clusterName, final List<ClusterNode> removedNodes) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null dataoutput");
        }
        if (removedNodes == null || removedNodes.isEmpty()) {
            return;
        }
        // write the header
        output.write(HEADER_NODES_REMOVED);
        // write the cluster count
        PackedInteger.writePackedInteger(output, 1);
        // write the cluster name
        output.writeUTF(clusterName);
        // write the removed nodes count
        final int removedNodesCount = removedNodes.size();
        PackedInteger.writePackedInteger(output, removedNodesCount);
        // write out the member info for each removed member
        for (final ClusterNode clusterMember : removedNodes) {
            // write the node name
            output.writeUTF(clusterMember.getName());
        }
    }
}
