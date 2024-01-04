/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.bmt;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;

/**
 *
 *
 * @author Stuart Douglas
 */
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class BMTSingleton {

    @Resource
    private EJBContext ejbContext;

    /**
     * This method leaks a transaction, and should result in an exception
     */
    public void leakTransaction(){
        try {
            ejbContext.getUserTransaction().begin();
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

}
