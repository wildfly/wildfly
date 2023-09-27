/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import jakarta.ejb.EJBLocalHome;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface StatefulSession30LocalHome extends EJBLocalHome {

    LocalStatefulSession30 create() throws jakarta.ejb.CreateException;

    LocalStatefulSession30 create(String value) throws jakarta.ejb.CreateException;

    LocalStatefulSession30 create(String value, Integer suffix) throws jakarta.ejb.CreateException;
}
