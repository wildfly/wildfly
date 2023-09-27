/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.classloading.custommodule;

import org.jboss.jca.adapters.jdbc.spi.ValidConnectionChecker;

import java.sql.Connection;
import java.sql.SQLException;

public class TestValidConnectionChecker implements ValidConnectionChecker {

    private static boolean invoked = false;

    @Override
    public SQLException isValidConnection(Connection c) {
        invoked = true;
        return null;
    }

    public static void reset() {
        invoked = false;
    }

    public static boolean wasInvoked() {
        return invoked;
    }
}
