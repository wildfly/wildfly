/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.removemethod;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

/**
 * @author Stuart Douglas
 */
@Stateful
public class Garage {

    public void park() {

    }

    @Remove
    public void remove() {

    }

}
