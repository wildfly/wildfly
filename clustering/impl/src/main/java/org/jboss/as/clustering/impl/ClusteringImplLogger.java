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

package org.jboss.as.clustering.impl;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.Collection;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.ClusteringApiLogger;
import org.jboss.as.clustering.GroupMembershipListener;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService.GroupView;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jgroups.Address;

/**
 * ClusteringImplLogger
 *
 * logging id range: 10220 - 10239
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ClusteringImplLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = ClusteringApiLogger.class.getPackage().getName();
    ClusteringImplLogger ROOT_LOGGER = Logger.getMessageLogger(ClusteringImplLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs a warning message indicating the partition, represented by the {@code groupName} parameter, message wrapper
     * does not contain an {@link Object Object[]} array.
     *
     * @param groupName the partition group name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10220, value = "Partition %s message wrapper does not contain Object[] object!")
    void invalidPartitionMessageWrapper(String groupName);

    /**
     * Logs a warning message indicating the partition, represented by the {@code groupName} parameter, message wrapper
     * does not contain an {@link org.jgroups.blocks.MethodCall MethodCall} object.
     *
     * @param groupName the partition group name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10221, value = "Partition %s message does not contain a MethodCall object!")
    void invalidPartitionMessage(String groupName);

    /**
     * Logs a warning message indicating the membership listener had a failed callback.
     *
     * @param cause    the cause of the error.
     * @param listener the listener in error.
     */
    @LogMessage(level = WARN)
    @Message(id = 10222, value = "Membership listener callback failure: %s")
    void membershipListenerCallbackFailure(@Cause Throwable cause, GroupMembershipListener listener);

    /**
     * Logs an error message indicating the method failed.
     *
     * @param cause      the cause of the error.
     * @param methodName the name of the method that failed.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10223, value = "%s failed")
    void methodFailure(@Cause Throwable cause, String methodName);

    /**
     * Logs an error message indicating the method failed for a service.
     *
     * @param cause       the cause of the error.
     * @param methodName  the name fo the method that failed.
     * @param serviceName the service name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10224, value = "%s failed for service %s")
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
    @Message(id = 10225, value = "New cluster view for partition %s (id: %d, delta: %d, merge: %b) : %s")
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
    @Message(id = 10226, value = "New cluster view for partition %s: %d (%s delta: %d, merge: %b)")
    void newClusterView(String groupName, long viewId, GroupView groupView, int difference, boolean isMerge);

    /**
     * Logs a warning message indicating the class represented by the {@code className} parameter was not registered to
     * receive state for the service represented by the {@code serviceName}.
     *
     * @param className   the class name where the service should be registered.
     * @param serviceName the service name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10227, value = "No %s registered to receive state for service %s")
    void notRegisteredToReceiveState(String className, String serviceName);

    /**
     * Logs a warning message indicating the partition, represented by the {@code groupName} parameter, message or the
     * message buffer is {@code null}.
     *
     * @param groupName the partition group name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10228, value = "Partition %s message or message buffer is null!")
    void nullPartitionMessage(String groupName);

    /**
     * Logs a warning message indicating the partition, represented by the {@code groupName} parameter, failed while
     * extracting the message body from the request bytes.
     *
     * @param cause     the cause of the error.
     * @param groupName the partition group name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10229, value = "Partition %s failed extracting message body from request bytes")
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
    @Message(id = 10230, value = "Partition %s failed deserializing message buffer (msg=%s)")
    void partitionFailedDeserializing(@Cause Throwable cause, String groupName, org.jgroups.Message request);

    /**
     * Logs a warning message indicating that concurrent requests were received to get service state.
     *
     * @param serviceName the service name the request was sent for.
     */
    @LogMessage(level = WARN)
    @Message(id = 10231, value = "Received concurrent requests to get service state for %s")
    void receivedConcurrentStateRequests(String serviceName);

    /**
     * Logs an informational message indicating the suspected member.
     *
     * @param suspectedMember the suspected member.
     */
    @LogMessage(level = INFO)
    @Message(id = 10232, value = "Suspected member: %s")
    void suspectedMember(Address suspectedMember);

    @LogMessage(level = WARN)
    @Message(id = 10233, value = "Failed to stop lock manager")
    void lockManagerStopFailed(@Cause Throwable cause);

    /**
     * Logs an error message indicating a throwable was caught during an asynchronous event.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10234, value = "Caught Throwable handling asynch events")
    void errorHandlingAsyncEvent(@Cause Throwable cause);

    /**
     * Logs an error message indicating the thread was interrupted.
     *
     * @param cause the cause of the error.
     * @param name  the name of the thread.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10235, value = "%s Thread interrupted")
    void threadInterrupted(@Cause Throwable cause, String name);

    /**
     * Logs a warning message indicating an exception was thrown invoking a method, represented by the
     * {@code methodName} parameter, on the service, represented by the {@code serviceName} parameter, asynchronously.
     *
     * @param cause       the cause of the error.
     * @param methodName  the method name.
     * @param serviceName the service name.
     */
    @LogMessage(level = WARN)
    @Message(id = 10236, value = "Caught exception asynchronously invoking method %s on service %s")
    void caughtErrorInvokingAsyncMethod(@Cause Throwable cause, String methodName, String serviceName);

    /**
     * Logs an error message indicating the property represented by the {@code propertyName} parameter failed to be set
     * from the service represented by the {@code serviceName} parameter.
     *
     * @param cause        the cause of the error.
     * @param propertyName the property name.
     * @param serviceName  the service name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10237, value = "failed setting %s for service %s")
    void failedSettingServiceProperty(@Cause Throwable cause, String propertyName, String serviceName);

    /**
     * Logs an informational message indicating the number of cluster members.
     *
     * @param size the number of cluster members.
     */
    @LogMessage(level = INFO)
    @Message(id = 10238, value = "Number of cluster members: %d")
    void numberOfClusterMembers(int size);
}
