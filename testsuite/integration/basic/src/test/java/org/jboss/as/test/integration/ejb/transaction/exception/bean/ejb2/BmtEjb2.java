/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

@Local(TestBean2Local.class)
@Remote(TestBean2Remote.class)
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BmtEjb2 {

    @Resource
    private UserTransaction utx;

    public void throwRuntimeException() {
        try {
            utx.begin();
            throw new RuntimeException();
        } catch (NotSupportedException | SystemException e) {
            throw new IllegalStateException("Can't begin transaction!", e);
        }
    }

}
