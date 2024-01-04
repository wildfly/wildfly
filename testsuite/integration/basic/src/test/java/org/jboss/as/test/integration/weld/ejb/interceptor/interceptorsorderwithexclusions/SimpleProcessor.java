/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.interceptor.interceptorsorderwithexclusions;

import jakarta.ejb.Stateless;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.Interceptors;

/**
 * @author Marius Bogoevici
 */
@Stateless
@Counted
@Interceptors(EjbInterceptor.class)
public class SimpleProcessor implements Processor {

    static int count;

    @Secured
    @Interceptors(EjbInterceptor2.class)
    public int add(int x, int y) {
        count = Counter.next();
        return x + y;
    }

    @Secured
    @ExcludeClassInterceptors
    public int subtract(int x, int y) {
        count = Counter.next();
        return x - y;
    }

}
