/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.stateful;

import jakarta.ejb.Stateful;

/**
 * User: jpai
 */
@Stateful
public class StatefulCalculator implements StatefulCalculatorRemote {

    private int currentValue;

    @Override
    public int add(int number) {
        currentValue += number;
        return currentValue;
    }
}
