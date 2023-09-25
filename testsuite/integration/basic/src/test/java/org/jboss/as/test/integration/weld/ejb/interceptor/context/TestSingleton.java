/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.context;

import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
public class TestSingleton {

    @EJB
    private HelloEJB helloEJB;

    //@PostConstruct
    public boolean test() {
        return helloEJB.sayHello() != null;
    }
}
