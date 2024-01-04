/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.support;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class A {
    private B b;

    public A(B b) {
        this.b = b;
    }

    public B getB() {
        return b;
    }
}
