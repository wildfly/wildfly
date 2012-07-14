/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.system;

/**
 * An interface describing a JBoss service MBean.
 *
 * @see Service
 * @see ServiceMBeanSupport
 * 
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:andreas@jboss.org">Andreas Schaefer</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81033 $
 */
public interface ServiceMBean
   extends Service
{
   // Constants -----------------------------------------------------

   /** ServiceController notification types corresponding to service lifecycle events */
   public static final String CREATE_EVENT  = "org.jboss.system.ServiceMBean.create";
   public static final String START_EVENT   = "org.jboss.system.ServiceMBean.start";
   public static final String STOP_EVENT    = "org.jboss.system.ServiceMBean.stop";
   public static final String DESTROY_EVENT = "org.jboss.system.ServiceMBean.destroy";

   public static final String[] states = {
      "Stopped", "Stopping", "Starting", "Started", "Failed",
      "Destroyed", "Created", "Unregistered", "Registered"
   };

   /** The Service.stop has completed */
   public static final int STOPPED  = 0;
   /** The Service.stop has been invoked */
   public static final int STOPPING = 1;
   /** The Service.start has been invoked */
   public static final int STARTING = 2;
   /** The Service.start has completed */
   public static final int STARTED  = 3;
   /** There has been an error during some operation */
   public static final int FAILED  = 4;
   /** The Service.destroy has completed */
   public static final int DESTROYED = 5;
   /** The Service.create has completed */
   public static final int CREATED = 6;
   /** The MBean has been created but has not completed MBeanRegistration.postRegister */
   public static final int UNREGISTERED = 7;
   /** The MBean has been created and has completed MBeanRegistration.postRegister */
   public static final int REGISTERED = 8;

   // Public --------------------------------------------------------
   
   String getName();
   int getState();
   String getStateString();
   
   /** Detyped lifecycle invocation */
   void jbossInternalLifecycle(String method) throws Exception;
}
