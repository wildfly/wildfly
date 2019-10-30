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

package org.jboss.as.txn.subsystem;

/**
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
interface CommonAttributes {

    String BINDING= "socket-binding";
    String CORE_ENVIRONMENT = "core-environment";
    String COORDINATOR_ENVIRONMENT = "coordinator-environment";
    String DEFAULT_TIMEOUT = "default-timeout";
    String MAXIMUM_TIMEOUT = "maximum-timeout";
    String ENABLE_STATISTICS = "enable-statistics";
    /** transaction status manager (TSM) service, needed for out of process recovery, should be provided or not */
    String ENABLE_TSM_STATUS = "enable-tsm-status";
    String NODE_IDENTIFIER = "node-identifier";
    String OBJECT_STORE = "object-store";
    String OBJECT_STORE_PATH = "object-store-path";
    String OBJECT_STORE_RELATIVE_TO = "object-store-relative-to";
    String STATE_STORE = "state";
    String COMMUNICATION_STORE = "communication";
    String ACTION_STORE = "action";

    String JTS = "jts";
    String USE_HORNETQ_STORE = "use-hornetq-store";
    String USE_JOURNAL_STORE = "use-journal-store";
    String HORNETQ_STORE_ENABLE_ASYNC_IO = "hornetq-store-enable-async-io";
    String JOURNAL_STORE_ENABLE_ASYNC_IO = "journal-store-enable-async-io";
    String JDBC_STORE = "jdbc-store";
    String USE_JDBC_STORE = "use-jdbc-store";
    String JDBC_STORE_DATASOURCE = "jdbc-store-datasource";
    String JDBC_ACTION_STORE_TABLE_PREFIX = "jdbc-action-store-table-prefix";
    String JDBC_ACTION_STORE_DROP_TABLE = "jdbc-action-store-drop-table";
    String JDBC_COMMUNICATION_STORE_TABLE_PREFIX = "jdbc-communication-store-table-prefix";
    String JDBC_COMMUNICATION_STORE_DROP_TABLE = "jdbc-communication-store-drop-table";
    String JDBC_STATE_STORE_TABLE_PREFIX = "jdbc-state-store-table-prefix";
    String JDBC_STATE_STORE_DROP_TABLE = "jdbc-state-store-drop-table";


    /** The com.arjuna.ats.arjuna.utils.Process implementation type */
    String PROCESS_ID = "process-id";
    String CONFIGURATION = "configuration";
    String LOG_STORE = "log-store";
    String RECOVERY_ENVIRONMENT = "recovery-environment";
    String RECOVERY_LISTENER = "recovery-listener";
    /** The process-id/socket element */
    String SOCKET = "socket";
    /** The process-id/socket attribute for max ports */
    String SOCKET_PROCESS_ID_MAX_PORTS = "max-ports";
    String STATISTICS_ENABLED = "statistics-enabled";
    String STATUS_BINDING = "status-socket-binding";
    /** The process-id/uuid element */
    String UUID = "uuid";
    // TxStats
    String NUMBER_OF_TRANSACTIONS = "number-of-transactions";
    String NUMBER_OF_NESTED_TRANSACTIONS = "number-of-nested-transactions";
    String NUMBER_OF_HEURISTICS = "number-of-heuristics";
    String NUMBER_OF_COMMITTED_TRANSACTIONS = "number-of-committed-transactions";
    String NUMBER_OF_ABORTED_TRANSACTIONS = "number-of-aborted-transactions";
    String NUMBER_OF_INFLIGHT_TRANSACTIONS = "number-of-inflight-transactions";
    String NUMBER_OF_TIMED_OUT_TRANSACTIONS = "number-of-timed-out-transactions";
    String NUMBER_OF_APPLICATION_ROLLBACKS = "number-of-application-rollbacks";
    String NUMBER_OF_RESOURCE_ROLLBACKS = "number-of-resource-rollbacks";
    String NUMBER_OF_SYSTEM_ROLLBACKS = "number-of-system-rollbacks";
    String AVERAGE_COMMIT_TIME = "average-commit-time";


    String PARTICIPANT = "participant";
    String TRANSACTION = "transaction";
    // TODO, process-id/mbean, process-id/file

    String CM_RESOURCES ="commit-markable-resources";
    String CM_RESOURCE ="commit-markable-resource";
    String CM_LOCATION ="xid-location";

    String CM_JNDI_NAME = "jndi-name";

    String CM_BATCH_SIZE = "batch-size";
    String CM_IMMEDIATE_CLEANUP = "immediate-cleanup";
    String CM_LOCATION_NAME = "name";

    Integer CM_BATCH_SIZE_DEF_VAL = 100;
    Boolean CM_IMMEDIATE_CLEANUP_DEF_VAL = true;
    String CM_LOCATION_NAME_DEF_VAL = "xids";
}
