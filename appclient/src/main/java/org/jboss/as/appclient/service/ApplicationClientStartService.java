/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.appclient.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;


/**
 * Service that is responsible for running an application clients main method, and shutting down the server once it
 * completes
 *
 * @author Stuart Douglas
 */
public class ApplicationClientStartService implements Service<ApplicationClientStartService> {


    public static final ServiceName SERVICE_NAME = ServiceName.of("appClientStart");

    private final InjectedValue<ApplicationClientDeploymentService> applicationClientDeploymentServiceInjectedValue = new InjectedValue<ApplicationClientDeploymentService>();
    private final Method mainMethod;
    private final String[] parameters;

    private Thread thread;

    private final Logger logger = Logger.getLogger(ApplicationClientStartService.class);

    public ApplicationClientStartService(final Method mainMethod, final String[] parameters) {
        this.mainMethod = mainMethod;
        this.parameters = parameters;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    applicationClientDeploymentServiceInjectedValue.getValue().getDeploymentCompleteLatch().await();
                    mainMethod.invoke(null,new Object[] { parameters});
                } catch (InvocationTargetException e) {
                    logger.error(e);
                } catch (IllegalAccessException e) {
                    logger.error(e);
                } catch (InterruptedException e) {
                    logger.error(e);
                } finally {
                    CurrentServiceContainer.getServiceContainer().shutdown();
                }
            }
        });
        thread.start();
    }

    @Override
    public synchronized void stop(final StopContext context) {
        thread.interrupt();
        thread = null;
    }

    @Override
    public ApplicationClientStartService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ApplicationClientDeploymentService> getApplicationClientDeploymentServiceInjectedValue() {
        return applicationClientDeploymentServiceInjectedValue;
    }
}
