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

package org.jboss.as.test.embedded.osgi.module;

import org.jboss.as.test.embedded.osgi.api.Echo;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class EchoInvokerService implements Service<Void>
{
   private static final Logger log = Logger.getLogger(EchoInvokerService.class);
   public static final ServiceName SERVICE_NAME = ServiceName.parse("jboss.osgi.xservice.invoker");

   private InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();

   public static void addService(ServiceTarget serviceTarget)
   {
      EchoInvokerService service = new EchoInvokerService();
      ServiceBuilder<?> serviceBuilder = serviceTarget.addService(SERVICE_NAME, service);
      serviceBuilder.addDependency(ServiceName.parse("jboss.osgi.context"), BundleContext.class, service.injectedBundleContext);
      serviceBuilder.setInitialMode(Mode.ACTIVE);
      serviceBuilder.install();
      log.infof("Service added: %s", SERVICE_NAME);
      log.infof("Echo Loader: %s", Echo.class.getClassLoader());
   }

   @Override
   public void start(StartContext context) throws StartException
   {
      BundleContext systemContext = injectedBundleContext.getValue();
      ServiceReference sref = systemContext.getServiceReference(Echo.class.getName());
      Echo service = (Echo)systemContext.getService(sref);
      service.echo("hello world");
   }

   @Override
   public void stop(StopContext context)
   {
   }

   @Override
   public Void getValue() throws IllegalStateException
   {
      return null;
   }
}
