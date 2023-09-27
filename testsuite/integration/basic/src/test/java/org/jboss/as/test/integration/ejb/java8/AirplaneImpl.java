/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.java8;

import jakarta.ejb.Stateful;

@Stateful
public class AirplaneImpl implements CargoPlane {

    @Override
    public boolean takeOff() {
        return true;
    }

}
