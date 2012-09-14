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

package org.jboss.as.test.integration.osgi.xservice.module;

import org.jboss.as.test.integration.osgi.xservice.api.Echo;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;

/**
 * A simple MSC service activator
 *
 * @author Thomas.Diesler@jboss.org
 * @since 09-Nov-2010
 */
public class ClientModuleOneActivator implements ServiceActivator
{
   public static final ServiceName INVOKER_SERVICE_NAME = ServiceName.JBOSS.append("osgi", "example", "invoker", "service");
   private static final Logger log = Logger.getLogger(ClientModuleOneActivator.class);

   @Override
   public void activate(ServiceActivatorContext context)
   {
      ServiceTarget serviceTarget = context.getServiceTarget();
      EchoInvokerService.addService(serviceTarget);

      ModuleClassLoader classLoader = (ModuleClassLoader)getClass().getClassLoader();
      ModuleIdentifier identifier = classLoader.getModule().getIdentifier();
      log.infof("ModuleIdentifier: %s", identifier);
   }

   static class EchoInvokerService extends AbstractService<Void>
   {
      private static final Logger log = Logger.getLogger(EchoInvokerService.class);

      private InjectedValue<Echo> injectedService = new InjectedValue<Echo>();

      static void addService(ServiceTarget serviceTarget)
      {
         EchoInvokerService service = new EchoInvokerService();
         ServiceBuilder<?> serviceBuilder = serviceTarget.addService(INVOKER_SERVICE_NAME, service);
         serviceBuilder.addDependency(EchoService.SERVICE_NAME, Echo.class, service.injectedService);
         serviceBuilder.setInitialMode(Mode.ACTIVE);
         serviceBuilder.install();
         log.infof("Service added: %s", INVOKER_SERVICE_NAME);
         log.infof("Echo Loader: %s", Echo.class.getClassLoader());
      }

      @Override
      public void start(StartContext context) throws StartException
      {
         Echo service = injectedService.getValue();
         service.echo("hello world");
      }
   }
}
