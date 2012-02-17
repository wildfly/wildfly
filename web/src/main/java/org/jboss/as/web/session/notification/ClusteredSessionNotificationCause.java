/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session.notification;

/**
 * Reasons why a servlet spec notification for a clustered session is being generated.
 * @author Brian Stansberry
 */
public enum ClusteredSessionNotificationCause {
    /**
     * Session has been newly created.
     */
    CREATE,

    /**
     * Session has been modified by the application.
     */
    MODIFY,

    /**
     * Session has failed over and is now in use on the local node.
     */
    FAILOVER,

    /**
     * Session has failed over and is no longer active on the local node.
     */
    FAILAWAY,

    /**
     * Session is being invalidated by the application.
     */
    INVALIDATE,

    /**
     * Session is being expired by the container due to timeout.
     */
    TIMEOUT,

    /**
     * Session is being expired by the container due to undeploy of the web application.
     */
    UNDEPLOY,

    /**
     * Local node became aware of a session active on another node as a result of the local node receiving a bulk state transfer
     * due to its being elected to provide backup for that other node's sessions.
     */
    STATE_TRANSFER,

    /**
     * The session is being passivated.
     */
    PASSIVATION,

    /**
     * The session is being activated.
     */
    ACTIVATION,

    /**
     * The session is being replicated.
     */
    REPLICATION,

    // /**
    // * Local node has taken "ownership" of a session for a reason other than
    // * failover; i.e. the session hasn't become active on the local node. In this
    // * case the local node would have become aware of the session earlier (i.e.
    // * via {@link #CREATE} or {@link #STATE_TRANSFER}) and these notifications
    // * would signal the local node taking greater responsibility for the session.
    // * Typically a policy implementation would not allow notifications for a
    // * remotely originated CREATE or for a STATE_TRANSFER if it allows
    // * notifications for TAKE_OWNERSHIP, and vice versa. Otherwise, multiple
    // * notifications would be received for the same session.
    // */
    // TAKE_OWNERSHIP,
    //
    // /**
    // * Local node has relinquished "ownership" of a session for a reason other than
    // * {@link #FAILAWAY} {@link #INVALIDATE}, {@link #TIMEOUT} or {@link #UNDEPLOY};
    // * i.e. some other node is taking over as the owner of session.
    // * Typically a policy implementation would not allow notifications for a
    // * remotely originated CREATE or for a STATE_TRANSFER if it allows
    // * notifications for TAKE_OWNERSHIP, and vice versa. Otherwise, multiple
    // * notifications would be received for the same session.
    // */
    // RELINQUISH_OWNERSHIP
    ;
}