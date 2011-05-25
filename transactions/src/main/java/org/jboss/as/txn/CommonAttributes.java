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

package org.jboss.as.txn;

/**
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
interface CommonAttributes {

    String BINDING= "socket-binding";
    String CORE_ENVIRONMENT = "core-environment";
    String COORDINATOR_ENVIRONMENT = "coordinator-environment";
    String DEFAULT_TIMEOUT = "default-timeout";
    String ENABLE_STATISTICS = "enable-statistics";
    String NODE_IDENTIFIER = "node-identifier";
    String OBJECT_STORE = "object-store";
    /** The com.arjuna.ats.arjuna.utils.Process implementation type */
    String PROCESS_ID = "process-id";
    String RECOVERY_ENVIRONMENT = "recovery-environment";
    /** The process-id/socket element */
    String SOCKET = "socket";
    /** The process-id/socket attribute for max ports */
    String SOCKET_PROCESS_ID_MAX_PORTS = "max-ports";
    String STATUS_BINDING = "status-socket-binding";
    /** The process-id/uuid element */
    String UUID = "uuid";
    // TODO, process-id/mbean, process-id/file
}
