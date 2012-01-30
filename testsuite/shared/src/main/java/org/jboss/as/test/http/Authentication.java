package org.jboss.as.test.http;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * @author Stuart Douglas
 */
public class Authentication {

    public static final String USERNAME = "testSuite";
    public static final String PASSWORD = "testSuitePassword";


    public static Authenticator getAuthenticator() {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD.toCharArray());
            }
        };
    }

    public static void setupDefaultAuthenticator() {
        Authenticator.setDefault(getAuthenticator());
    }
}
