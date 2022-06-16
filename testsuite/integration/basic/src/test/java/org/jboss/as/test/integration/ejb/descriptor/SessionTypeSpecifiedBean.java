package org.jboss.as.test.integration.ejb.descriptor;

import jakarta.ejb.Singleton;

/**
 * @author Stuart Douglas
 */
@Singleton
public class SessionTypeSpecifiedBean {

    private int value;

    public int increment() {
        return value++;
    }

}
