/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.rar;

import org.jboss.logging.Logger;

/**
 * HelloWorldConnectionImpl
 *
 * @version $Revision: $
 */
public class HelloWorldConnectionImpl implements HelloWorldConnection {
   /** The logger */
   private static Logger log = Logger.getLogger("HelloWorldConnectionImpl");

   private final HelloWorldManagedConnectionFactory mcf;

   /**
    * default constructor
    */
   public HelloWorldConnectionImpl(HelloWorldManagedConnectionFactory mcf) {
      this.mcf = mcf;
   }

   /**
    * call helloWorld
    */
   public String helloWorld() {
      return helloWorld(((HelloWorldResourceAdapter)mcf.getResourceAdapter()).getName());
   }

   /**
    * call helloWorld
    */
   public String helloWorld(String name) {
      return "Hello World, " + name + " !";
   }
}
