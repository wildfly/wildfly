/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static java.util.UUID.randomUUID;

public class DefaultCredentials {
    private static String username = null;
    private static String password = null;

    public static synchronized String getUsername() {
        if (username == null) {
            username = randomUUID().toString();
        }

        return username;
    }

    public static synchronized String getPassword() {
        if (password == null) {
            password = randomUUID().toString();
        }

        return password;
    }
}
