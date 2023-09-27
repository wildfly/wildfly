/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.support;

import org.jboss.as.pojo.api.BeanFactory;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class D {

    public void create(Object bf) throws Throwable {
        Method create = bf.getClass().getMethod("create");
        B b = (B) create.invoke(bf);
    }

    public void start(Object bf) throws Throwable {
        BeanFactory tbf = (BeanFactory) bf;
        B b = (B) tbf.create();
    }

}
