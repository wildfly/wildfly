/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.serialization;

import jakarta.ejb.Stateful;

/**
 * stateful session bean
 *
 */
@Stateful
public class AnnotatedBean {

    private int value;

    public int getValue() {
        return value;
    }

    public void setValue(final int value) {
        this.value = value;
    }
}
