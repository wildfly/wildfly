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

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

import java.io.Serializable;

/**
 * Date: 26.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ClusteringApiMessages {
    /**
     * The messages.
     */
    ClusteringApiMessages MESSAGES = Messages.getBundle(ClusteringApiMessages.class);

    /**
     * Creates an exception indicating a raw throwable was caught on a remote invocation.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10220, value = "Caught raw Throwable on remote invocation")
    RuntimeException caughtRemoteInvocationThrowable(@Cause Throwable cause);

    /**
     * Creates an exception indicating a variable is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10221, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating the variable represented the {@code name} parameter must be set before invoking
     * the method represented by the {@code methodName}.
     *
     * @param name       the variable name.
     * @param methodName the method name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10222, value = "Must set %s before calling %s")
    IllegalStateException varNotSet(String name, String methodName);

    /**
     * A message indicating a lock could not be acquired from a cluster.
     *
     * @param lockName the lock name.
     *
     * @return the message.
     */
    @Message(id = 10223, value = "Cannot acquire lock %s from cluster")
    String cannotAcquireLock(Serializable lockName);

    /**
     * A message indicating that a lock could not be acquired because it's held by an unknown.
     *
     * @return the message.
     */
    @Message(id = 10224, value = "Unable to acquire lock as it is held by unknown")
    String cannotAcquireHeldLock();

    /**
     * A message indicating that a lock could not be acquired because it's held by the node.
     *
     * @param node the node that holds the lock.
     *
     * @return the message.
     */
    @Message(id = 10225, value = "Unable to acquire lock as it is held by %s")
    String cannotAcquireHeldLock(ClusterNode node);

    /**
     * Creates an exception indicating an incompatible dispatcher.
     *
     * @param dispatcherName the dispatcher class name.
     * @param dispatcher     the dispatcher.
     * @param notifierName   the notifier class name.
     * @param notifier       the notifier.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10226, value = "%s %s is not compatible with %s %s")
    IllegalArgumentException incompatibleDispatcher(String dispatcherName, GroupRpcDispatcher dispatcher, String notifierName, GroupMembershipNotifier notifier);

    /**
     * Creates an exception indicating the first method must be called before the second method.
     *
     * @param firstMethod  the first method that should be called.
     * @param secondMethod the second method.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10227, value = "Must call %s before first call to %s")
    IllegalStateException invalidMethodCall(String firstMethod, String secondMethod);

    /**
     * Creates an exception indicating an unlock should not be invoked for remote nodes.
     *
     * @param caller the caller.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10228, value = "Should not receive unlock calls for remote node %s")
    IllegalStateException receivedUnlockForRemoteNode(ClusterNode caller);

    /**
     * Creates an exception indicating the failure to release a remote lock.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10229, value = "Failed releasing remote lock")
    RuntimeException remoteLockReleaseFailure(@Cause Throwable cause);
}
