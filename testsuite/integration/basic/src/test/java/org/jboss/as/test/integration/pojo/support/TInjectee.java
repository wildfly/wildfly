/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.support;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TInjectee {
    private final String prefix;

    public TInjectee() {
        this("Hello, ");
    }

    public TInjectee(String prefix) {
        this.prefix = prefix;
    }

    public void sayHello(String msg) {
        System.out.println(prefix + msg);
    }
}
