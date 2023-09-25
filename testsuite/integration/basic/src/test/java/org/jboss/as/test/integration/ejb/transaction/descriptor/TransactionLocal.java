/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.descriptor;

import jakarta.ejb.Local;

/**
 * @author Stuart Douglas
 */
@Local
public interface TransactionLocal {

    int transactionStatus();


    int transactionStatus2();

}
