/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.aroundtimeout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;

/**
 * @author Stuart Douglas
 */
public class InterceptorOrder {


    private static final List<Class<?>> order = new ArrayList<Class<?>>();

    public static void reset() {
        order.clear();
    }

    public static void intercept(Class<?> type) {
        order.add(type);
    }

    public static void assertEquals(Class ... expected) {
        Assertions.assertEquals(Arrays.asList(expected), order);
    }


}
