/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.packaging;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
@CdiInterceptorBinding
public class CdiEjb {

    public String sayHello() {
        return "Hello";
    }

}
