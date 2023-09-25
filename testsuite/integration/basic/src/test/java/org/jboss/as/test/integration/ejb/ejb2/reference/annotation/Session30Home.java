/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import jakarta.ejb.EJBHome;

import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session30;


/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Session30Home extends EJBHome {

    Session30 create() throws java.rmi.RemoteException, jakarta.ejb.CreateException;
}
