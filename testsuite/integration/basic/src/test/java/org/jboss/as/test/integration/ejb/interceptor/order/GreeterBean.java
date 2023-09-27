/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.order;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@Interceptors({ FirstCustomInterceptor.class, SecondCustomInterceptor.class })
public class GreeterBean implements GreeterRemote {
    public String sayHi(String name) {
        return "Hi " + name;
    }
}
