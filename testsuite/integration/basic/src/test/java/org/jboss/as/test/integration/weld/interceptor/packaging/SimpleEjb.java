/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.interceptor.packaging;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
@Intercepted
public class SimpleEjb {

    public String sayHello() {
        return "Hello";
    }

}
