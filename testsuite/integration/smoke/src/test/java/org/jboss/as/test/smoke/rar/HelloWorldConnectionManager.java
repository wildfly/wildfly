/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.smoke.rar;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;

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
