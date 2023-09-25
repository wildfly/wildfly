/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.localhome;

import jakarta.ejb.EJBLocalHome;

/**
 *
 * @author Stuart Douglas
 */
public interface SimpleStatefulLocalHome extends EJBLocalHome{

    SimpleLocalInterface createSimple(String message);

    SimpleLocalInterface createComplex(String first, String second);

}
