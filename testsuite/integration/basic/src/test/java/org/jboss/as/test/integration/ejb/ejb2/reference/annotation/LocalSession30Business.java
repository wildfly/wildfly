/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import java.rmi.RemoteException;

/**
 * LocalSession30Business
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
public interface LocalSession30Business {
    String access() throws RemoteException;
}
