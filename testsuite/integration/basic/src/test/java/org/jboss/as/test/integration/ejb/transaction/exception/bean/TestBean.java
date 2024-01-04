/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception.bean;

import org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException;

public interface TestBean {

    void throwRuntimeException();

    void throwExceptionFromTm(TxManagerException txManagerException) throws Exception;
}
