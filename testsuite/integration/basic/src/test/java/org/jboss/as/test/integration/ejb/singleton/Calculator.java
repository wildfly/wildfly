/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton;

import jakarta.ejb.Singleton;

/**
 * Calculator
 *
 * @author Jaikiran Pai
 */
@Singleton
public class Calculator {
    public int subtract(int a, int b) {
        return a - b;
    }

    public int add(int a, int b) {
        return a + b;
    }
}
