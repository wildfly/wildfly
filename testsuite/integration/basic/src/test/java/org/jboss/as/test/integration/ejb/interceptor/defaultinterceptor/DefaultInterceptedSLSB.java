/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.defaultinterceptor;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
@LocalBean
public class DefaultInterceptedSLSB implements SessionBean {

    private boolean postConstructCalled;

    public String message() {
        return "Hello";
    }

    @Override
    public void setPostConstructCalled() {
        postConstructCalled = true;
    }

    public boolean isPostConstructCalled() {
        return postConstructCalled;
    }
}
