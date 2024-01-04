/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.method;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Ondrej Chaloupka
 */
@Stateless
@Interceptors({AroundInvokeInterceptor.class})
public class AroundInvokeBean {

 public String call() {
     return "Hi";
 }
}
