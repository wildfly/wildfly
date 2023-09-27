/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb3;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.TestBean;

@LocalBean
@Remote
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BmtEjb3 implements TestBean {

    @Resource
    private UserTransaction utx;

    @Override
    public void throwRuntimeException() {
        try {
            utx.begin();
            throw new RuntimeException();
        } catch (NotSupportedException | SystemException e) {
            throw new IllegalStateException("Can't begin transaction!", e);
        }
    }

    @Override
    public void throwExceptionFromTm(TxManagerException txManagerException) throws Exception {
        throw new UnsupportedOperationException();
    }

}
