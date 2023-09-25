/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Stateless
public class MySession2Bean implements MySession2, MySession2Local {
    public boolean doit() {
        return true;
    }
}
