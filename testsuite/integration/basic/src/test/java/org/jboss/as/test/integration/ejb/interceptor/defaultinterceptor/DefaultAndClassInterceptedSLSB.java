/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.defaultinterceptor;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@LocalBean
@Interceptors({ ClassInterceptor.class })
public class DefaultAndClassInterceptedSLSB implements SessionBean {

    private boolean postConstructCalled;

    public String defaultAndClassIntercepted() {
        return "Hello";
    }

    public String noClassAndDefaultInDescriptor() {
        return "Hi";
    }

    @Override
    public void setPostConstructCalled() {
        postConstructCalled = true;
    }

    public boolean isPostConstructCalled() {
        return postConstructCalled;
    }
}
