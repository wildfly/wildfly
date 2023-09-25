/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.interceptor.packaging;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
@Intercepted
public class SimpleEjb2 {

    private String postConstructMessage;

    public String sayHello() {
        return "Hello";
    }

    public String getPostConstructMessage() {
        return postConstructMessage;
    }

    public void setPostConstructMessage(String postConstructMessage) {
        this.postConstructMessage = postConstructMessage;
    }
}
