/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.multipleinterceptors;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless(name="bean2")
@Interceptors(MyInterceptor.class)
public class Bean2 {
}
