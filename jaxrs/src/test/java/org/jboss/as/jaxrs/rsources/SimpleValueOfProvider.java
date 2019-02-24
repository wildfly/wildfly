package org.jboss.as.jaxrs.rsources;

public class SimpleValueOfProvider {

    public static String valueOf(String s) {
        throw new RuntimeException("Force error");
    }
}
