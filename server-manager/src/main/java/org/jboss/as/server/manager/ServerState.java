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
package org.jboss.as.server.manager;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public enum ServerState {
    /** We have told the PM to start the server and are waiting for the SERVER_AVAILABLE message back from the server */
    BOOTING (true),

    /** The server has sent the available command back to the SERVER_MANAGER */
    AVAILABLE (true),

    /** We have received the SERVER_AVAILABLE message from the server process and have sent the config
     *  to the server and are waiting for the SERVER_STARTED or SERVER_FAILED message */
    STARTING (false),

    /** The server sent back the SERVER_STARTED message and is up and running */
    STARTED (false),

    /** We have told the server to stop and are waiting for the SERVER_STOPPED message */
    STOPPING (false),

    /** We have received the SERVER_STOPPED message */
    STOPPED (false),

    /** We have received the SERVER_START_FAILED message */
    FAILED (true),

    /** We have tried to restart the server several times and received the SERVER_START_FAILED message
     * more times than defined in the max respawn policy */
    MAX_FAILED (false);

    private final boolean restartOnReconnect;

    private ServerState(boolean restartedReconnect) {
        this.restartOnReconnect = restartedReconnect;
    }

    boolean isRestartOnReconnect() {
        return restartOnReconnect;
    }
}
