package org.jboss.as.test.integration.deployment.classloading.WFLY_8907;

import java.io.Serializable;

public class MyObject implements Serializable {

    private static final long serialVersionUID = 3208868686459967210L;

    private String stuff;

    public MyObject(String stuff) {
        this.stuff = stuff;
    }

    public String stuff() {
        return stuff;
    }
}
