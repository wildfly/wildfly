/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.stateful;

import jakarta.ejb.EJB;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

/**
 *
 */
@Stateful
public class StatefulAddingBean implements RemoteInterface {


    @EJB
    private StatefulCalculatorRemote statefulCalculator;

    private int value = 0;

    @Override
    public void add(final int i) {
        this.value = this.statefulCalculator.add(i);
    }

    @Override
    public int get() {
        return value;
    }

    @Override
    public ValueWrapper getValue() {
        return new ValueWrapper();
    }

    @Override
    @Remove
    public void remove() {

    }
}
