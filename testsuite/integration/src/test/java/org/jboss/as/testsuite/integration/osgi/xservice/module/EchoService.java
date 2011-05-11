/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.osgi.xservice.module;

import org.jboss.as.testsuite.integration.osgi.xservice.api.Echo;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A simple MSC service that provides an echo method
 *
 * @author Thomas.Diesler@jboss.org
 * @since 09-Nov-2010
 */
public class EchoService implements Echo, Service<Echo>
{
   private static final Logger log = Logger.getLogger(EchoService.class);
   public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "example", "target", "service");

   public static void addService(ServiceTarget serviceTarget)
   {
      // Add the service with an alias that the OSGi layer can use to lookup the service
      ServiceBuilder<Echo> serviceBuilder = serviceTarget.addService(SERVICE_NAME, new EchoService());
      serviceBuilder.addAliases(ServiceName.of("jbosgi", "xservice", Echo.class.getName()));
      serviceBuilder.setInitialMode(Mode.ACTIVE);
      serviceBuilder.install();
      log.infof("Service added: %s", SERVICE_NAME);
      log.infof("Echo Loader: %s", Echo.class.getClassLoader());
   }

   @Override
   public String echo(String message)
   {
      log.infof("Echo: %s", message);
      return message;
   }

   @Override
   public void start(StartContext context) throws StartException
   {
   }

   @Override
   public void stop(StopContext context)
   {
   }

   @Override
   public Echo getValue() throws IllegalStateException
   {
      return this;
   }
}
