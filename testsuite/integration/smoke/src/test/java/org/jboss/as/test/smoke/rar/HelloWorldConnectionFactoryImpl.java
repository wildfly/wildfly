/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.rar;

import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

/**
 * HelloWorldConnectionFactoryImpl
 *
 * @version $Revision: $
 */
public class HelloWorldConnectionFactoryImpl implements HelloWorldConnectionFactory {
   private static final long serialVersionUID = 1L;

   private Reference reference;

   private final HelloWorldManagedConnectionFactory mcf;
   private final ConnectionManager connectionManager;

   /**
    * default constructor
    * @param   mcf       ManagedConnectionFactory
    * @param   cxManager ConnectionManager
    */
   public HelloWorldConnectionFactoryImpl(HelloWorldManagedConnectionFactory mcf,
                                          ConnectionManager cxManager) {
      this.mcf = mcf;
      this.connectionManager = cxManager;
   }

   /**
    * get connection from factory
    *
    * @return HelloWorldConnection instance
    * @exception ResourceException Thrown if a connection can't be obtained
    */
   @Override
   public HelloWorldConnection getConnection() throws ResourceException {
      return new HelloWorldConnectionImpl(mcf);
   }

   /**
    * Get the Reference instance.
    *
    * @return Reference instance
    * @exception NamingException Thrown if a reference can't be obtained
    */
   @Override
   public Reference getReference() throws NamingException {
      return reference;
   }

   /**
    * Set the Reference instance.
    *
    * @param   reference  A Reference instance
    */
   @Override
   public void setReference(Reference reference) {
      this.reference = reference;
   }
}
