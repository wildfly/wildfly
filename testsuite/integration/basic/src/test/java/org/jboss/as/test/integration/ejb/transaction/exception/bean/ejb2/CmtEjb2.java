/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.transaction.TransactionManager;

@Local(TestBean2Local.class)
@Remote(TestBean2Remote.class)
@Stateless
public class CmtEjb2 {

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    public void throwRuntimeException() {
        throw new RuntimeException();
    }

}
