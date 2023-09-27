/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.defaultinterceptor;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.ExcludeDefaultInterceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@LocalBean
@ExcludeDefaultInterceptors
public class NoDefaultInterceptorsSLSB implements SessionBean {

    private boolean postConstructCalled;

    public String message() {
        return "Hello";
    }

    @ExcludeClassInterceptors
    public String noClassLevel() {
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
