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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.Collection;

import org.jboss.as.clustering.CoreGroupCommunicationService.GroupView;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jgroups.Address;

/**
 * Date: 26.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
interface ClusteringImplLogger extends ClusteringApiLogger {

    /**
     * Logs an error message indicating an error occurred while destroying the service.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10240, value = "Error destroying service")
    void errorDestroyingService(@Cause Throwable cause);

    /**
     * Logs a warning message indicating an exception occurred while stopping.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 10241, value = "Exception in stop")
    void exceptionInStop(@Cause Throwable cause);

    /**
     * Logs a warning message indicating the partition, represented by the {@code groupName} parameter, message wrapper
     * does not contain an {@link Object Object[]} array.
     *
     * @param groupName the partition group name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10242, value = "Partition %s message wrapper does not contain Object[] object!")
    void invalidPartitionMessageWrapper(String groupName);

    /**
     * Logs a warning message indicating the partition, represented by the {@code groupName} parameter, message wrapper
     * does not contain an {@link org.jgroups.blocks.MethodCall MethodCall} object.
     *
     * @param groupName the partition group name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10243, value = "Partition %s message does not contain a MethodCall object!")
    void invalidPartitionMessage(String groupName);

    /**
     * Logs a warning message indicating the membership listener had a failed callback.
     *
     * @param cause    the cause of the error.
     * @param listener the listener in error.
     */
    @LogMessage(level = WARN)
    @Message(id = 10244, value = "Membership listener callback failure: %s")
    void memberShipListenerCallbackFailure(@Cause Throwable cause, GroupMembershipListener listener);

    /**
     * Logs an error message indicating the method failed.
     *
     * @param cause      the cause of the error.
     * @param methodName the name of the method that failed.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10245, value = "%s failed")
    void methodFailure(@Cause Throwable cause, String methodName);

    /**
     * Logs an error message indicating the method failed for a service.
     *
     * @param cause       the cause of the error.
     * @param methodName  the name fo the method that failed.
     * @param serviceName the service name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10246, value = "%s failed for service %s")
    void methodFailure(@Cause Throwable cause, String methodName, String serviceName);

    /**
     * Logs an informational message with information on a new cluster view.
     *
     * @param groupName  the group name.
     * @param viewId     the new view id.
     * @param difference the size difference between the old group and the new group.
     * @param isMerge    {@code true} if this was a merge view, otherwise {@code false}.
     * @param allMembers a collection of all the new members.
     */
    @LogMessage(level = INFO)
    @Message(id = 10247, value = "New cluster view for partition %s (id: %d, delta: %d, merge: %b) : %s")
    void newClusterCurrentView(String groupName, long viewId, int difference, boolean isMerge, Collection<ClusterNode> allMembers);

    /**
     * Logs an informational message with information on a new cluster view.
     *
     * @param groupName  the group name.
     * @param viewId     the new view id.
     * @param groupView  the group view.
     * @param difference the size difference between the old group and the new group.
     * @param isMerge    {@code true} if this was a merge view, otherwise {@code false}.
     */
    @LogMessage(level = INFO)
    @Message(id = 10248, value = "New cluster view for partition %s: %d (%s delta: %d, merge: %b)")
    void newClusterView(String groupName, long viewId, GroupView groupView, int difference, boolean isMerge);

    /**
     * Logs a warning message indicating the class represented by the {@code className} parameter was not registered to
     * receive state for the service represented by the {@code serviceName}.
     *
     * @param className   the class name where the service should be registered.
     * @param serviceName the service name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10249, value = "No %s registered to receive state for service %s")
    void notRegisteredToReceiveState(String className, String serviceName);

    /**
     * Logs a warning message indicating the partition, represented by the {@code groupName} parameter, message or the
     * message buffer is {@code null}.
     *
     * @param groupName the partition group name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10250, value = "Partition %s message or message buffer is null!")
    void nullPartitionMessage(String groupName);

    /**
     * Logs a warning message indicating the partition, represented by the {@code groupName} parameter, failed while
     * extracting the message body from the request bytes.
     *
     * @param cause     the cause of the error.
     * @param groupName the partition group name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10251, value = "Partition %s failed extracting message body from request bytes")
    void partitionFailedExtractingMessageBody(@Cause Throwable cause, String groupName);

    /**
     * Logs an informational message indicating the partition, represented by the {@code groupName} parameter, failed
     * to unserialize the message buffer, represented by the {@code request} parameter.
     *
     * @param cause     the cause of the error.
     * @param groupName the partition group name.
     * @param request   the message.
     */
    @LogMessage(level = WARN)
    @Message(id = 10252, value = "Partition %s failed unserializing message buffer (msg=%s)")
    void partitionFailedUnserialing(@Cause Throwable cause, String groupName, org.jgroups.Message request);

    /**
     * Logs a warning message indicating that concurrent requests were received to get service state.
     *
     * @param serviceName the service name the request was sent for.
     */
    @LogMessage(level = WARN)
    @Message(id = 10253, value = "Received concurrent requests to get service state for %s")
    void receivedConcurrentStateRequests(String serviceName);

    /**
     * Logs an informational message indicating the suspected member.
     *
     * @param suspectedMember the suspected member.
     */
    @LogMessage(level = INFO)
    @Message(id = 10254, value = "Suspected member: %s")
    void suspectedMember(Address suspectedMember);
}
