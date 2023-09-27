/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.classloading.custommodule;

import org.jboss.jca.adapters.jdbc.spi.ExceptionSorter;

import java.sql.SQLException;

public class TestExceptionSorter implements ExceptionSorter {

    private static boolean invoked = false;

    @Override
    public boolean isExceptionFatal(SQLException e) {
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
