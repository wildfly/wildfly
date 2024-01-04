/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hibernate.jpa.test.pack.war;

/**
 * @author Emmanuel Bernard
 */
public class OtherIncrementListener {
    private static int increment;

    public static int getIncrement() {
        return OtherIncrementListener.increment;
    }

    public static void reset() {
        increment = 0;
    }

    public void increment(Object entity) {
        OtherIncrementListener.increment++;
    }
}
