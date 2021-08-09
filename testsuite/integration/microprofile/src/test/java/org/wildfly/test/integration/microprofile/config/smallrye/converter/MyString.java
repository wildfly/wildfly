package org.wildfly.test.integration.microprofile.config.smallrye.converter;

/**
 * Simple String wrapper to avoid converting all Strings
 */
public class MyString {

    public String value;

    public static MyString from(String value) {
        MyString myString = new MyString();
        myString.value = value;
        return myString;
    }
}
