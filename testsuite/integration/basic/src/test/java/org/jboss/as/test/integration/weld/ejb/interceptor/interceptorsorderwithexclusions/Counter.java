/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.interceptor.interceptorsorderwithexclusions;

/**
 * @author Marius Bogoevici
 */
public class Counter {
    static int count;

    public static int next() {
        return ++count;
    }
}
