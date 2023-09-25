/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.validation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DummyClass {
    @NotNull
    private String direction;

    @Min(1)
    protected int speed;

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int number) {
        this.speed = number;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
