/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import java.util.ArrayList;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Stateless
@Interceptors(MyInterceptor.class)
public class MySessionBean implements MySessionRemote {

    public ArrayList doit() {
        throw new RuntimeException("SHOULD NEVER BE CALLED");
    }
}
