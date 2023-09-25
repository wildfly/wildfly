/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.localhome;

import jakarta.ejb.EJBLocalObject;

/**
 * @author Stuart Douglas
 */
public interface SimpleLocalInterface extends EJBLocalObject {

    String sayHello();

    String otherMethod();
}
