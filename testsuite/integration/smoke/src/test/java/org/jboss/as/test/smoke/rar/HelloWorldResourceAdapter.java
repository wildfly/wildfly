/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.rar;

import org.jboss.logging.Logger;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.TransactionSupport;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * HelloWorldResourceAdapter
 *
 * @version $Revision: $
 */
@Connector(
   reauthenticationSupport = false,
   transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction)
public class HelloWorldResourceAdapter implements ResourceAdapter {
   /** The logger */
   private static Logger log = Logger.getLogger("HelloWorldResourceAdapter");

   /** Name property */
   @ConfigProperty(defaultValue="AS 7", supportsDynamicUpdates=true)
   private String name;

   /**
    * default constructor
    */
   public HelloWorldResourceAdapter() {
   }

   /**
    * set name
    * @param name The value
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * get name
    * @return The value
    */
   public String getName() {
      return name;
   }

   /**
    * This is called during the activation of a message endpoint.
    *
    * @param endpointFactory a message endpoint factory instance.
    * @param spec an activation spec JavaBean instance.
    * @throws ResourceException generic exception
    */
   public void endpointActivation(MessageEndpointFactory endpointFactory,
                                  ActivationSpec spec) throws ResourceException {
   }

   /**
    * This is called when a message endpoint is deactivated.
    *
    * @param endpointFactory a message endpoint factory instance.
    * @param spec an activation spec JavaBean instance.
    */
   public void endpointDeactivation(MessageEndpointFactory endpointFactory,
                                    ActivationSpec spec) {
   }

   /**
    * This is called when a resource adapter instance is bootstrapped.
    *
    * @param ctx a bootstrap context containing references
    * @throws ResourceAdapterInternalException indicates bootstrap failure.
    */
   public void start(BootstrapContext ctx)
      throws ResourceAdapterInternalException {
   }

   /**
    * This is called when a resource adapter instance is undeployed or
    * during application server shutdown.
    */
   public void stop() {
   }

   /**
    * This method is called by the application server during crash recovery.
    *
    * @param specs an array of ActivationSpec JavaBeans
    * @throws ResourceException generic exception
    * @return an array of XAResource objects
    */
   public XAResource[] getXAResources(ActivationSpec[] specs)
      throws ResourceException {
      return null;
   }

   /**
    * Returns a hash code value for the object.
    * @return a hash code value for this object.
    */
   @Override
   public int hashCode() {
      int result = 17;
      if (name != null)
         result += 31 * result + 7 * name.hashCode();
      else
         result += 31 * result + 7;
      return result;
   }

   /**
    * Indicates whether some other object is equal to this one.
    * @param   other   the reference object with which to compare.
    * @return true if this object is the same as the obj argument; false otherwise.
    */
   @Override
   public boolean equals(Object other) {
      if (other == null)
         return false;
      if (other == this)
         return true;
      if (!(other instanceof HelloWorldResourceAdapter))
         return false;
      HelloWorldResourceAdapter obj = (HelloWorldResourceAdapter)other;
      boolean result = true;
      if (result) {
         if (name == null)
            result = obj.getName() == null;
         else
            result = name.equals(obj.getName());
      }
      return result;
   }
}
