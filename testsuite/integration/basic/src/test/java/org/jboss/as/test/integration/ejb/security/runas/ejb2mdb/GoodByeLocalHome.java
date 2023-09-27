/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import jakarta.ejb.CreateException;
import jakarta.ejb.EJBLocalHome;

/**
 * @author Ondrej Chaloupka
 */
public interface GoodByeLocalHome extends EJBLocalHome {
    GoodByeLocal create() throws CreateException;
}
