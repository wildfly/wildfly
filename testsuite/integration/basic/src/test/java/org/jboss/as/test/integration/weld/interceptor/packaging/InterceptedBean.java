/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.interceptor.packaging;

/**
 * @author Stuart Douglas
 */
@Intercepted
public class InterceptedBean  {

    public String sayHello() {
        return "Hello";
    }
}
