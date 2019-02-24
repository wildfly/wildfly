package org.jboss.as.jaxrs.rsources;

public class SimpleFromStringProvider {

    public static String fromString(String s) {
        throw new RuntimeException("Force error");
    }
}