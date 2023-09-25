/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

public interface TestBean2Remote extends EJBObject {

    void throwRuntimeException() throws RemoteException;
}
