package org.jboss.as.jaxrs.rsources;

public class SimpleFromValueProvider {

    public String fromValue(String s) {
        throw new RuntimeException("Force error");
    }
}
