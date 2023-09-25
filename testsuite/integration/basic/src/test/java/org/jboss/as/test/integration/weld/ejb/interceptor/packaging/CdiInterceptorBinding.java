/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.packaging;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Stuart Douglas
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface CdiInterceptorBinding {
}
