/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.rar;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnectionFactory;

/**
 * HelloWorldConnectionManager
 *
 * @version $Revision: $
 */
public class HelloWorldConnectionManager implements ConnectionManager {
   private static final long serialVersionUID = 1L;

/**
    * default constructor
    */
   public HelloWorldConnectionManager() {

   }

   /**
    * Allocate a connection
    *
    * @param mcf The managed connection factory
    * @param cri The connection request information
    * @return Object The connection
    * @exception ResourceException Thrown if an error occurs
    */
   @Override
   public Object allocateConnection(ManagedConnectionFactory mcf,ConnectionRequestInfo cri) throws ResourceException {
      return null;
   }


}
