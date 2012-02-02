package org.jboss.as.messaging;

import static java.util.UUID.randomUUID;

public class HornetQDefaultCredentials {
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
