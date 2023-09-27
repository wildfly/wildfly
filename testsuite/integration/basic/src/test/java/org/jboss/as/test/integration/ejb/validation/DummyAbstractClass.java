/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.validation;

import jakarta.validation.constraints.Min;

public abstract class DummyAbstractClass {
    @Min(1)
    protected int speed;

    public abstract int getSpeed();

    public abstract void setSpeed(int speed);
}
