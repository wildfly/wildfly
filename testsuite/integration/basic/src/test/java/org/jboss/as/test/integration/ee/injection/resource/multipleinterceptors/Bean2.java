/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.multipleinterceptors;

import jakarta.annotation.ManagedBean;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@ManagedBean("bean2")
@Interceptors(MyInterceptor.class)
public class Bean2 {
}
