/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

import jakarta.interceptor.Interceptors;

/**
 * @author <a href="mailto:amay@ingenta.com">Andrew May</a>
 */
@Interceptors({InterceptA.class})
public abstract class ABean implements A {
    public String getMessage() {
        return "The Message";
    }
}
