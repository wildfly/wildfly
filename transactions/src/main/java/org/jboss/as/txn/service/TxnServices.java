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

package org.jboss.as.txn.service;

import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 */
public final class TxnServices {

    public static final ServiceName JBOSS_TXN = ServiceName.JBOSS.append("txn");

    public static final ServiceName JBOSS_TXN_PATHS = JBOSS_TXN.append("paths");

    public static final ServiceName JBOSS_TXN_CORE_ENVIRONMENT = JBOSS_TXN.append("CoreEnvironment");

    public static final ServiceName JBOSS_TXN_XA_TERMINATOR = JBOSS_TXN.append("XATerminator");

    public static final ServiceName JBOSS_TXN_EXTENDED_JBOSS_XA_TERMINATOR = JBOSS_TXN.append("ExtendedJBossXATerminator");

    public static final ServiceName JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT = JBOSS_TXN.append("ArjunaObjectStoreEnvironment");

    public static final ServiceName JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER = JBOSS_TXN.append("ArjunaTransactionManager");

    public static final ServiceName JBOSS_TXN_ARJUNA_RECOVERY_MANAGER = JBOSS_TXN.append("ArjunaRecoveryManager");

    public static final ServiceName JBOSS_TXN_TRANSACTION_MANAGER = JBOSS_TXN.append("TransactionManager");

    public static final ServiceName JBOSS_TXN_USER_TRANSACTION = JBOSS_TXN.append("UserTransaction");

    public static final ServiceName JBOSS_TXN_USER_TRANSACTION_REGISTRY = JBOSS_TXN.append("UserTransactionRegistry");

    public static final ServiceName JBOSS_TXN_SYNCHRONIZATION_REGISTRY = JBOSS_TXN.append("TransactionSynchronizationRegistry");

    public static final ServiceName JBOSS_TXN_JTA_ENVIRONMENT = JBOSS_TXN.append("JTAEnvironment");

    public static final ServiceName JBOSS_TXN_CMR = JBOSS_TXN.append("CMR");

    public static final ServiceName JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT = JBOSS_TXN.append("context", "local");

    public static final ServiceName JBOSS_TXN_REMOTE_TRANSACTION_SERVICE = JBOSS_TXN.append("service", "remote");

    public static final ServiceName JBOSS_TXN_HTTP_REMOTE_TRANSACTION_SERVICE = JBOSS_TXN.append("service", "http-remote");

    public static final ServiceName JBOSS_TXN_CONTEXT_XA_TERMINATOR = JBOSS_TXN.append("JBossContextXATerminator");


    public static <T> T notNull(T value) {
        if (value == null) throw TransactionLogger.ROOT_LOGGER.serviceNotStarted();
        return value;
    }

    private TxnServices() {
    }
}
