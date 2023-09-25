/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.servlet;

import jakarta.ejb.EJBLocalHome;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Session30LocalHome extends EJBLocalHome {
    Session30Local create() throws jakarta.ejb.CreateException;
}
