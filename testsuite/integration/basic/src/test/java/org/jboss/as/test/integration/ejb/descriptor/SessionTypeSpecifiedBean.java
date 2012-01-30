package org.jboss.as.test.integration.ejb.descriptor;

import javax.ejb.Singleton;

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
