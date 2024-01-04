/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

import java.rmi.RemoteException;
import jakarta.ejb.EJBObject;

/**
 * An Ejb21View.
 *
 * @author <a href="arubinge@redhat.com">ALR</a>
 */
public interface Ejb21View extends EJBObject {
    String test() throws RemoteException;
}
