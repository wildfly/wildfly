/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.context;

import jakarta.ejb.Stateless;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
@Startup
@ServiceLogged
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HelloEJB {

    public String sayHello() {
        return "hello";
    }
}
