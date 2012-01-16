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

import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.network.ClientMapping;
import org.jboss.ejb.client.remoting.PackedInteger;

import java.io.DataOutput;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * @param output                   The {@link DataOutput} into which the message will be written
     * @param clientMappingsRegistries
     * @throws IOException
     */
    void writeCompleteClusterTopology(final DataOutput output, final Collection<Registry<String, List<ClientMapping>>> clientMappingsRegistries) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null dataoutput");
        }
        if (clientMappingsRegistries == null || clientMappingsRegistries.isEmpty()) {
            return;
        }
        // write the header
        output.write(HEADER_COMPLETE_CLUSTER_TOPOLOGY);
        // write the cluster count
        PackedInteger.writePackedInteger(output, clientMappingsRegistries.size());
        // write out each of the cluster's topology
        for (final Registry<String, List<ClientMapping>> registry : clientMappingsRegistries) {
            // write the cluster name
            output.writeUTF(registry.getName());
            // write out the information of each cluster node
            this.writeClusterNodes(output, registry.getName(), registry.getEntries());
        }

    }

    /**
     * Writes out a EJB remoting protocol message containing the names of the <code>clusters</code> which have been removed
     * from the server.
     *
     * @param output     The {@link DataOutput} to which the message will be written
     * @param registries
     * @throws IOException
     */
    void writeClusterRemoved(final DataOutput output, final Collection<Registry<String, List<ClientMapping>>> registries) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null dataoutput");
        }
        if (registries == null || registries.isEmpty()) {
            return;
        }
        // write the header
        output.write(HEADER_CLUSTER_REMOVED);
        // write the cluster count
        PackedInteger.writePackedInteger(output, registries.size());
        // write out the cluster name for each of the removed cluster
        for (final Registry<String, List<ClientMapping>> registry : registries) {
            output.writeUTF(registry.getName());
        }
    }

    void writeNewNodesAdded(final DataOutput output, final String clusterName,
                            final Map<String, List<ClientMapping>> clientMappings) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null dataoutput");
        }
        if (clientMappings.isEmpty()) {
            return;
        }
        // write the header
        output.write(HEADER_NEW_NODES_ADDED);
        // write the cluster count
        PackedInteger.writePackedInteger(output, 1);
        // write the cluster name
        output.writeUTF(clusterName);
        // write out the cluster node(s) information
        this.writeClusterNodes(output, clusterName, clientMappings);
    }

    void writeNodesRemoved(final DataOutput output, final String clusterName, final Set<String> removedNodes) throws IOException {
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
        for (final String node : removedNodes) {
            // write the node name
            output.writeUTF(node);
        }
    }

    private void writeClusterNodes(final DataOutput output, final String clusterName, final Map<String, List<ClientMapping>> clientMappings) throws IOException {
        // write the member node count
        final int memberCount = clientMappings.size();
        PackedInteger.writePackedInteger(output, memberCount);
        // write out the member info for each member
        for (final Map.Entry<String, List<ClientMapping>> entry : clientMappings.entrySet()) {
            // write the node name
            final String nodeName = entry.getKey();
            output.writeUTF(nodeName);
            // write the client-mapping count
            final List<ClientMapping> clientMappingsForNode = entry.getValue();
            if (clientMappingsForNode == null || clientMappingsForNode.isEmpty()) {
                throw new IllegalStateException("No client-mapping entries found for node " + nodeName + " in cluster " + clusterName);
            }
            PackedInteger.writePackedInteger(output, clientMappingsForNode.size());

            // for each client-mapping write out the mapping info
            for (final ClientMapping clientMapping : clientMappingsForNode) {
                final InetAddress clientNetworkAddress = clientMapping.getSourceNetworkAddress();
                // client netmask + 1 bit for differentiating IPv6/IPv4 source address
                final int netMask = clientMapping.getSourceNetworkMaskBits();
                final int netMaskAndIPFamilyDifferentiator;
                if (clientNetworkAddress instanceof Inet4Address) {
                    // add a trailing 1 bit to indicate it's a ipv4 address
                    netMaskAndIPFamilyDifferentiator = (netMask << 1) | 1;
                } else {
                    // add a trailing 0 bit to indicate it's a ipv6 address
                    netMaskAndIPFamilyDifferentiator = (netMask << 1);
                }
                // write the netMask with the ipv6/ipv4 differentiator
                PackedInteger.writePackedInteger(output, netMaskAndIPFamilyDifferentiator);

                // write client source network address
                output.write(clientNetworkAddress.getAddress());
                // destination address
                output.writeUTF(clientMapping.getDestinationAddress());
                // destination port
                output.writeShort(clientMapping.getDestinationPort());
            }
        }
    }
}
