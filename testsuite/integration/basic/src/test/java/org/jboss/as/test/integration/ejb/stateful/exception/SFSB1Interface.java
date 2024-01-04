/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.exception;

public interface SFSB1Interface {
    void systemException();
    void ejbException();
    void userException() throws TestException;
    void remove();
}
