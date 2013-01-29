/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.expression;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.server.Services;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The idea and the most part of the code taken from: 
 * http://management-platform.blogspot.cz/2012/07/co-located-management-client-for.html
 * The service loading problem seems to still exist: https://issues.jboss.org/browse/AS7-5172
 */
public class ExpressionTestManagementService implements ServiceActivator {
       private static final Logger log = Logger.getLogger(ExpressionTestManagementService.class);
       private static volatile ModelController controller;
       private static volatile ExecutorService executor;


       public static ModelControllerClient getTestExpressionClient() {
          ModelControllerClient client = controller.createClient(executor);
          log.debug("Returning controller: " + controller + " and client " + client);
          return client;
       }


       @Override
       public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
          log.info("Activating service " + ExpressionTestManagementService.class.getName());
          final GetModelControllerService service = new GetModelControllerService();
          context
              .getServiceTarget()
              .addService(ServiceName.of("expresion-test-management", "client", "getter"), service)
              .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.modelControllerValue)
              .install();
       }


       private class GetModelControllerService implements Service<Void> {
          private final Logger log = Logger.getLogger(GetModelControllerService.class);
          private InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();


          @Override
          public Void getValue() throws IllegalStateException, IllegalArgumentException {
             return null;
          }


          @Override
          public void start(StartContext context) throws StartException {
             ExpressionTestManagementService.executor = Executors.newFixedThreadPool(5, new ThreadFactory() {
                 @Override
                 public Thread newThread(Runnable r) {
                     Thread t = new Thread(r);
                     t.setDaemon(true);
                     t.setName("ManagementServiceModelControllerClientThread");
                     return t;
                 }
             });
             ExpressionTestManagementService.controller = modelControllerValue.getValue();
             log.info(GetModelControllerService.class.getSimpleName() + " service started");
          }


          @Override
          public void stop(StopContext context) {
             try {
                ExpressionTestManagementService.executor.shutdownNow();
             } finally {
                ExpressionTestManagementService.executor = null;
                ExpressionTestManagementService.controller = null;
             }
             log.info(GetModelControllerService.class.getSimpleName() + " service stopped");
          }
       }
    }

