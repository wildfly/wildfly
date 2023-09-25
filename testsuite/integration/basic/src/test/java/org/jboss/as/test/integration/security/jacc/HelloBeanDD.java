/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.jacc;


/**
 * A HelloBean.
 *
 * @author Josef Cacek
 */
public class HelloBeanDD {

    public static final String HELLO_WORLD = "Hello world!";

    // Public methods --------------------------------------------------------

    /**
     * Returns {@value #HELLO_WORLD}.
     *
     * @see org.jboss.as.test.integration.security.common.ejb3.Hello#sayHelloWorld()
     */
    public String sayHello() {
        return HELLO_WORLD;
    }

    /**
     * Returns echo of the given string (2x repeated).
     *
     * @see org.jboss.as.test.integration.security.common.ejb3.Hello#sayHello()
     */
    public String echo(String name) {
        return name + name;
    }

}
