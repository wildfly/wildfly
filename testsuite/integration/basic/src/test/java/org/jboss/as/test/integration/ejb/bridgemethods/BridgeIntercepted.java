/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.bridgemethods;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import jakarta.interceptor.InterceptorBinding;

/**
 * @author Stuart Douglas
 */
@Retention(RetentionPolicy.RUNTIME)
@InterceptorBinding
public @interface BridgeIntercepted {
}
