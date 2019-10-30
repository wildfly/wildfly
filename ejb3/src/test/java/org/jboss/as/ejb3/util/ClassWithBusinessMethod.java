package org.jboss.as.ejb3.util;

public class ClassWithBusinessMethod implements BusinessInterface {

    @Override
    public void businessMethod(String argumentOne, int argumentTwo) {
        // ...
    }

    @Override
    public void businessMethod(String argumentOne, int argumentTwo, boolean argumentThree) {

    }

    public void notABusinessMethod(String argumentOne) {

    }
}
