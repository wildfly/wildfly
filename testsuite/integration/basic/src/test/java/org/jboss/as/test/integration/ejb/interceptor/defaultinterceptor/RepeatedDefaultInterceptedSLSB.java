/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.defaultinterceptor;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.ExcludeDefaultInterceptors;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@LocalBean
@Interceptors({ DefaultInterceptor.class, DefaultInterceptor.class })
public class RepeatedDefaultInterceptedSLSB implements SessionBean {

    private boolean postConstructCalled;

    public String message() {
        return "Hello";
    }

    @ExcludeClassInterceptors
    public String noClassLevel() {
        return "Hello";
    }

    @ExcludeClassInterceptors
    @ExcludeDefaultInterceptors
    public String noClassLevelOrDefault() {
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
