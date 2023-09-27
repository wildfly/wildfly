/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.rar;

/**
 * User: jpai
 */
public class HelloWorldConnectionImpl implements HelloWorldConnection {


    /**
     * ManagedConnection
     */

    private HelloWorldManagedConnection mc;


    /**
     * ManagedConnectionFactory
     */

    private HelloWorldManagedConnectionFactory mcf;


    /**
     * Default constructor
     *
     * @param mc  HelloWorldManagedConnection
     * @param mcf HelloWorldManagedConnectionFactory
     */
    public HelloWorldConnectionImpl(HelloWorldManagedConnection mc, HelloWorldManagedConnectionFactory mcf) {

        this.mc = mc;

        this.mcf = mcf;

    }


    /**
     * Call helloWorld
     *
     * @return String helloworld
     */
    public String helloWorld() {

        return helloWorld(mcf.getResourceAdapter().toString());

    }


    /**
     * Call helloWorld
     *
     * @param name String name
     * @return String helloworld
     */
    public String helloWorld(String name) {

        return "Hello World, " + name + " !";

    }


    /**
     * Close
     */
    public void close() {

        mc.closeHandle(this);

    }

}

