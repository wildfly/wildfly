/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.descriptor;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

/**
 * @author Stuart Douglas
 */
@Stateless
public class DescriptorBean implements TransactionLocal, TransactionRemote {

    @Resource(lookup="java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    @Override
    public int transactionStatus() {
        try {
            return transactionManager.getStatus();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int transactionStatus2() {
        try {
            return transactionManager.getStatus();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}
