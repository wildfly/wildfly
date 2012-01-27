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

package org.jboss.as.clustering;

import java.util.Collection;

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jgroups.Address;

/**
 * Date: 26.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ClusteringImplMessages extends ClusteringApiMessages {

    /**
     * The messages.
     */
    ClusteringImplMessages MESSAGES = Messages.getBundle(ClusteringImplMessages.class);

    /**
     * Creates an exception indicating the address was not registered with the transport layer.
     *
     * @param address the address that was not registered.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10280, value = "Address %s not registered in transport layer")
    IllegalStateException addressNotRegistered(Address address);

    /**
     * Creates an exception indicating a duplicate view was found.
     *
     * @param newNode     the new cluster node.
     * @param currentNode the current cluster node that matches the {@code newNode}.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10281, value = "Found member %s in current view that duplicates us (%s). This node cannot join partition until duplicate member has  been removed")
    IllegalStateException duplicateViewFound(ClusterNode newNode, ClusterNode currentNode);

    /**
     * Creates an exception indicating a failure to create a multiplexed channel.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10282, value = "Failure creating multiplexed Channel")
    RuntimeException failedToCreateMultiplexChannel(@Cause Throwable cause);

    /**
     * Creates an exception indicating a {@code HAPartitionConfig} does not have the type.
     *
     * @param type the type that configuration does not have.
     *
     * @return an {@code IllegalStateException} for the error.
     */
    @Message(id = 10283, value = "HAPartitionConfig has no %s")
    IllegalStateException haPartitionConfigHasNo(String type);

    /**
     * Creates an exception indicating the initial transfer failed.
     *
     * @param transferName the transfer name.
     *
     * @return an {@link IllegalStateException} for he error.
     */
    @Message(id = 10284, value = "Initial %s transfer failed")
    IllegalStateException initialTransferFailed(String transferName);

    /**
     * Creates an exception indicating the target node is not an instance of the type parameter.
     *
     * @param targetNode the target node.
     * @param type       the acceptable type.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10285, value = "targetNode %s is not an instance of %s -- only targetNodes provided by this HAPartition should be used")
    IllegalArgumentException invalidTargetNodeInstance(ClusterNode targetNode, Class<? extends ClusterNode> type);

    /**
     * A message indicating a suspected node.
     *
     * @param suspectedMember the suspected member.
     *
     * @return the message.
     */
    @Message(id = 10286, value = "Node suspected: %s")
    String nodeSuspected(Address suspectedMember);

    /**
     * Creates an exception indicating the state transfer for the {@code serviceName} parameter will return an input
     * stream is already pending.
     *
     * @param serviceName the service name.
     * @param returnType  the return type requested.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10288, value = "State transfer task for %s that will return an %s is already pending")
    IllegalStateException stateTransferAlreadyPending(String serviceName, String returnType);

    /**
     * A message indicating a new view was created.
     *
     * @param allMembers all the members in the view.
     * @param id         the view id.
     * @param oldView    the old view.
     *
     * @return the message.
     */
    @Message(id = 10290, value = "New view: %s with viewId: %d (old view: %s)")
    String viewCreated(Collection<ClusterNode> allMembers, long id, CoreGroupCommunicationService.GroupView oldView);

    /**
     * Creates an exception indicating that no view was received from the object name represented by the
     * {@code fromObjectName} parameter.
     *
     * @param fromObjectName the from object name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10291, value = "No view received from %s")
    IllegalStateException viewNotReceived(String fromObjectName);
}
