/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.constructor;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Stateless
public class NoDefaultCtorBean implements NoDefaultCtorView {

    private final Dog dog;

    @Inject
    private NoDefaultCtorBean(Dog dog) {
        this.dog = dog;
    }

    @Override
    public Dog getDog() {
        return dog;
    }
}
