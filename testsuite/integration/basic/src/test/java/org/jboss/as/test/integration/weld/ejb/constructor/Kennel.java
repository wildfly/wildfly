/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.constructor;

import jakarta.ejb.Stateful;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Stateful
public class Kennel {

    private final Dog dog;

    public Kennel() {
        dog = null;
    }

    @Inject
    public Kennel(Dog dog) {
        this.dog = dog;
    }

    public Dog getDog() {
        return dog;
    }
}
