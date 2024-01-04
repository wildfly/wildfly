/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.resource;

import jakarta.annotation.Resource;
import javax.naming.Context;
import jakarta.transaction.TransactionManager;

/**
 *
 * @author Eduardo Martins
 *
 */
public class X implements XMBean {

    private TransactionManager transactionManager;

    @Resource(lookup = "java:jboss/mail")
    private Context context;

    @Resource(lookup = "java:/TransactionManager")
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public boolean resourcesInjected() {
        return transactionManager != null && context != null;
    }

}
