/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import jakarta.ejb.EJBException;
import jakarta.ejb.EJBLocalObject;

/**
 * @author Ondrej Chaloupka
 */
public interface GoodByeLocal extends EJBLocalObject {
    String sayGoodBye() throws EJBException;
}
