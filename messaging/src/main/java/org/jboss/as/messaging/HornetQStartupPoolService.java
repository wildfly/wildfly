/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.messaging;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Pool used to execute potentially blocking startup tasks.
 *
 * @author Jason T. Greene
 */
public class HornetQStartupPoolService implements Service<Executor>{
    private volatile ExecutorService executor;

     public static ServiceController<?> addService(final ServiceTarget target, ServiceName hqServiceName, final ServiceListener<Object>... listeners) {
        final HornetQStartupPoolService service = new HornetQStartupPoolService();
        return target.addService(MessagingServices.getHornetQStartupPoolServiceName(hqServiceName), service)
            .addListener(listeners)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public synchronized void stop(StopContext context) {
        executor.shutdown();
        executor = null;
    }

    @Override
    public Executor getValue() throws IllegalStateException, IllegalArgumentException {
        return executor;
    }
}
