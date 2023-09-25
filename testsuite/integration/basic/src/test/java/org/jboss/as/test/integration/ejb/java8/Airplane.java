/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.java8;

import jakarta.ejb.Local;

@Local
public interface Airplane {

    static boolean barrelRoll() {
        return true;
    }

    boolean takeOff();

    String getPlaneType();
}
