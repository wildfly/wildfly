/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.packaging.war.namingcontext;

import javax.naming.NamingException;
import jakarta.transaction.UserTransaction;

/**
 * @author Stuart Douglas
 */
public interface EjbInterface {
    UserTransaction lookupUserTransaction() throws NamingException;

    UserTransaction lookupOtherUserTransaction() throws NamingException;
}
