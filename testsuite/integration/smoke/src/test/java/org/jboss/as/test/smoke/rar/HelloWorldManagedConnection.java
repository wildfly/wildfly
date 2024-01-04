/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.rar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

/**
 * HelloWorldManagedConnection
 *
 * @version $Revision: $
 */
public class HelloWorldManagedConnection implements ManagedConnection {
   /** The logger */
   private static Logger log = Logger.getLogger("HelloWorldManagedConnection");

   /** MCF */
   private final HelloWorldManagedConnectionFactory mcf;

   /** Log writer */
   private PrintWriter logWriter;

   /** Listeners */
   private final List<ConnectionEventListener> listeners;

   /** Connection */
   private Object connection;

   /**
    * default constructor
    * @param mcf mcf
    */
   public HelloWorldManagedConnection(HelloWorldManagedConnectionFactory mcf) {
      this.mcf = mcf;
      this.logWriter = null;
      this.listeners = new ArrayList<ConnectionEventListener>(1);
      this.connection = null;
   }

   /**
    * Creates a new connection handle for the underlying physical connection
    * represented by the ManagedConnection instance.
    *
    * @param        subject        security context as JAAS subject
    * @param        cxRequestInfo  ConnectionRequestInfo instance
    * @return       generic Object instance representing the connection handle.
    * @throws  ResourceException     generic exception if operation fails
    */
   public Object getConnection(Subject subject,
                               ConnectionRequestInfo cxRequestInfo) throws ResourceException {
      connection = new HelloWorldConnectionImpl(mcf);

      return connection;
   }

   /**
    * Used by the container to change the association of an
    * application-level connection handle with a ManagedConnection instance.
    *
    * @param   connection  Application-level connection handle
    * @throws  ResourceException     generic exception if operation fails
    */
   public void associateConnection(Object connection) throws ResourceException {
      this.connection = connection;
   }

   /**
    * Application server calls this method to force any cleanup on the ManagedConnection instance.
    *
    * @throws    ResourceException     generic exception if operation fails
    */
   public void cleanup() throws ResourceException {
   }

   /**
    * Destroys the physical connection to the underlying resource manager.
    *
    * @throws    ResourceException     generic exception if operation fails
    */
   public void destroy() throws ResourceException {
      this.connection = null;
   }

   /**
    * Adds a connection event listener to the ManagedConnection instance.
    *
    * @param  listener   a new ConnectionEventListener to be registered
    */
   public void addConnectionEventListener(ConnectionEventListener listener) {
      if (listener == null)
         throw new IllegalArgumentException("Listener is null");

      listeners.add(listener);
   }

   /**
    * Removes an already registered connection event listener from the ManagedConnection instance.
    *
    * @param  listener   already registered connection event listener to be removed
    */
   public void removeConnectionEventListener(ConnectionEventListener listener) {
      if (listener == null)
         throw new IllegalArgumentException("Listener is null");

      listeners.remove(listener);
   }

   /**
    * Gets the log writer for this ManagedConnection instance.
    *
    * @return  Character output stream associated with this Managed-Connection instance
    * @throws ResourceException     generic exception if operation fails
    */
   public PrintWriter getLogWriter() throws ResourceException {
      return logWriter;
   }

   /**
    * Sets the log writer for this ManagedConnection instance.
    *
    * @param      out        Character Output stream to be associated
    * @throws     ResourceException  generic exception if operation fails
    */
   public void setLogWriter(PrintWriter out) throws ResourceException {
      this.logWriter = out;
   }

   /**
    * Returns an <code>jakarta.resource.spi.LocalTransaction</code> instance.
    *
    * @return     LocalTransaction instance
    * @throws ResourceException     generic exception if operation fails
    */
   public LocalTransaction getLocalTransaction() throws ResourceException {
      throw new NotSupportedException("LocalTransaction not supported");
   }

   /**
    * Returns an <code>javax.transaction.xa.XAResource</code> instance.
    *
    * @return     XAResource instance
    * @throws ResourceException     generic exception if operation fails
    */
   public XAResource getXAResource() throws ResourceException {
      throw new NotSupportedException("GetXAResource not supported");
   }

   /**
    * Gets the metadata information for this connection's underlying EIS resource manager instance.
    *
    * @return ManagedConnectionMetaData instance
    * @throws ResourceException     generic exception if operation fails
    */
   public ManagedConnectionMetaData getMetaData() throws ResourceException {
      return new HelloWorldManagedConnectionMetaData();
   }
}
