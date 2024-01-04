/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso.interfaces;

import jakarta.ejb.CreateException;
import jakarta.ejb.EJBLocalHome;

/**
 * A trivial local SessionBean home interface.
 *
 * @author Scott.Stark@jboss.org
 */
public interface StatelessSessionLocalHome extends EJBLocalHome {
    StatelessSessionLocal create() throws CreateException;
}
