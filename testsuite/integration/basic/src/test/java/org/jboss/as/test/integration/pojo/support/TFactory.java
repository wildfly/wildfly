/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.support;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TFactory {
    private final String msg;

    public TFactory(String msg) {
        this.msg = msg;
    }

    public static TBean createBean(String msg) {
        return new TBean(msg);
    }

    public TInjectee defaultInjectee() {
        return new TInjectee(msg);
    }

    public TInjectee defaultInjectee(int x) {
        return new TInjectee(String.valueOf(x));
    }
}
