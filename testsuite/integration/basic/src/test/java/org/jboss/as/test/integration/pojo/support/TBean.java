/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.support;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TBean {
    private String msg;
    private TInjectee injectee;

    public TBean() {
        this("Hello, ");
    }

    public TBean(String msg) {
        this.msg = msg;
    }

    public TInjectee getInjectee() {
        return injectee;
    }

    public void setInjectee(TInjectee injectee) {
        this.injectee = injectee;
    }

    public void create() {
        if (injectee != null)
            injectee.sayHello("world!");
    }

    public void start(String anotherMsg) {
        if (injectee != null)
            injectee.sayHello(anotherMsg);
    }

    public void stop(String anotherMsg) {
        if (injectee != null)
            injectee.sayHello(anotherMsg);
    }

    public void destroy() {
        if (injectee != null)
            injectee.sayHello("actually bye!");
    }

    public void install(String msg) {
        if (injectee != null)
            injectee.sayHello(this.msg + msg);
    }
}
