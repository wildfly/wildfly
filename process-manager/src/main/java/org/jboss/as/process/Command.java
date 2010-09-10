/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.process;

/**
 * Commands sent between the Process Manager (PM) and other processes. Other
 * processes are either the Process Manager (PM) or Server instances.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Command {
    /** Tells the process manager to add a process (SM->PM) */
    ADD,

    /** Tells the process manager to start a process (SM->PM) */
    START,

    /** Tells the process manager to stop a process (SM->PM) */
    STOP,

    /** Tells the process manager to remove a process (SM->PM) */
    REMOVE,

    /** Send a message via the process manager (Process->PM) */
    SEND,

    /** Send a message via the process manager (Process->PM) */
    SEND_BYTES,

    /** Broadcast a message via the process manager (Process->PM) */
    BROADCAST,

    /** Broadcast a message via the process manager (Process->PM) */
    BROADCAST_BYTES,

    /** Shutdown a process (PM->Process) */
    SHUTDOWN,

    /** Shutdown all the known servers (PM->SM) */
    SHUTDOWN_SERVERS,

    /** All the known servers have been shut down (SM->PM) */
    SERVERS_SHUTDOWN,

    /** The SM has been restarted, tell all server processes to reconnect (SM->PM)*/
    RECONNECT_SERVERS,

    /** Reconnect to the SM (PM->SM) */
    RECONNECT_SERVER_MANAGER,

    /** Receive a message from another process or process manager (PM->Process) */
    MSG,

    /** Reeive a message from another process or process manager (PM->Process) */
    MSG_BYTES,

    /** Sent by PM if when a Process is determined to be down (PM->SM) */
    DOWN
    ;
}
