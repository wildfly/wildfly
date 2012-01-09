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

import java.io.IOException;
import java.util.Map;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ProcessMessageHandler {

    void handleProcessAdded(ProcessControllerClient client, String processName);

    void handleProcessStarted(ProcessControllerClient client, String processName);

    void handleProcessStopped(ProcessControllerClient client, String processName, long uptimeMillis);

    void handleProcessRemoved(ProcessControllerClient client, String processName);

    void handleProcessInventory(ProcessControllerClient client, Map<String, ProcessInfo> inventory);

    void handleConnectionShutdown(ProcessControllerClient client);

    void handleConnectionFailure(ProcessControllerClient client, IOException cause);

    void handleConnectionFinished(ProcessControllerClient client);

    void handleOperationFailed(ProcessControllerClient client, OperationType operation, String processName);

    public enum OperationType {

        ADD(Protocol.ADD_PROCESS),
        INVENTORY(Protocol.REQUEST_PROCESS_INVENTORY),
        REMOVE(Protocol.REMOVE_PROCESS),
        RECONNECT(Protocol.RECONNECT_PROCESS),
        SEND_STDIN(Protocol.SEND_STDIN),
        START(Protocol.START_PROCESS),
        STOP(Protocol.STOP_PROCESS),
        ;

        final int code;

        private OperationType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        static OperationType fromCode(int code) {
            for(OperationType type : values()) {
                if( type.getCode() == code) {
                    return type;
                }
            }
            return null;
        }
    }

}
