/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author <a href="mailto:amay@ingenta.com">Andrew May</a>
 */
@Stateless
@Remote(B.class)
@Interceptors({ InterceptB.class })
public class BBean extends ABean implements B {
    public String getOtherMessage() {
        return "The Other Message";
    }
}
