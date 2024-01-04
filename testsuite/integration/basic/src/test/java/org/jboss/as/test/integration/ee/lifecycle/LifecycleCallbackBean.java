/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * User: jpai
 */
@Stateless
@Interceptors(SimpleInterceptor.class)
public class LifecycleCallbackBean {

    private boolean postConstructInvoked;

    @PostConstruct
    private void onConstruct() {
        this.postConstructInvoked = true;
    }

    public boolean wasPostConstructInvoked() {
        return this.postConstructInvoked;
    }
}
