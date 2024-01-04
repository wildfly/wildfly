/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.localhome;

import jakarta.ejb.EJBLocalHome;

/**
 * Simple local home interface
 *
 * @author Stuart Douglas
 */
public interface SimpleLocalHome extends EJBLocalHome {

    SimpleLocalInterface createSimple();

}
