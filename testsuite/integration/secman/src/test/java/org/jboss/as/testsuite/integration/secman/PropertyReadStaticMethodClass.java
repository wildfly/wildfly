package org.jboss.as.testsuite.integration.secman;

public class PropertyReadStaticMethodClass {

    public static String readProperty(String property) {
        return System.getProperty(property);
    }
}
