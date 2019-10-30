/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

