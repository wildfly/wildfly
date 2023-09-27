/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.rar;

/**
 * User: jpai
 */
public interface HelloWorldConnection {
    /**
     * HelloWorld
     *
     * @return String
     */

    String helloWorld();


    /**
     * HelloWorld
     *
     * @param name A name
     * @return String
     */

    String helloWorld(String name);


    /**
     * Close
     */

    void close();
}
