/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.rar;

/**
 * HelloWorldConnection
 *
 * @version $Revision: $
 */
public interface HelloWorldConnection {
   /**
    * helloWorld
    * @return String
    */
   String helloWorld();

   /**
    * helloWorld
    * @param name name
    * @return String
    */
   String helloWorld(String name);
}
