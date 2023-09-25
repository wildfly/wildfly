/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.classloading.custommodule;

import org.jboss.jca.adapters.jdbc.spi.StaleConnectionChecker;

import java.sql.SQLException;

public class TestStaleConnectionChecker implements StaleConnectionChecker {

    private static boolean invoked = false;

    @Override
    public boolean isStaleConnection(SQLException e) {
        invoked = true;
        return false;
    }

    public static void reset() {
        invoked = false;
    }

    public static boolean wasInvoked() {
        return invoked;
    }
}
