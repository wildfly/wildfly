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

/**
 * Date: 26.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ClusteringApiMessages extends ClusteringMessages {
    /**
     * The messages.
     */
    ClusteringApiMessages MESSAGES = Messages.getBundle(ClusteringApiMessages.class);

    /**
     * A message indicating that a lock could not be acquired because it's held by an unknown.
     *
     * @return the message.
     */
    @Message(value = "Unable to acquire lock as it is held by unknown")
    String cannotAcquireHeldLock();

    /**
     * A message indicating that a lock could not be acquired because it's held by the node.
     *
     * @param node the node that holds the lock.
     *
     * @return the message.
     */
    @Message(value = "Unable to acquire lock as it is held by %s")
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
    @Message(value = "%s %s is not compatible with %s %s")
    IllegalArgumentException incompatibleDispatcher(String dispatcherName, GroupRpcDispatcher dispatcher, String notifierName, GroupMembershipNotifier notifier);

    /**
     * Creates an exception indicating an unlock should not be invoked for remote nodes.
     *
     * @param caller the caller.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(value = "Should not receive unlock calls for remote node %s")
    IllegalStateException receivedUnlockForRemoteNode(ClusterNode caller);

    /**
     * Creates an exception indicating the failure to release a remote lock.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(value = "Failed releasing remote lock")
    RuntimeException remoteLockReleaseFailure(@Cause Throwable cause);
}
