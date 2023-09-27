/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * Provides access to TSR + TM
 *
 * @author Scott Marlow
 */
public interface JtaManager {

    TransactionSynchronizationRegistry getSynchronizationRegistry();

    TransactionManager locateTransactionManager();
}
