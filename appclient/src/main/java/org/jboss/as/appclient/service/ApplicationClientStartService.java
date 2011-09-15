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

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Service that is responsible for running an application clients main method, and shutting down the server once it
 * completes
 *
 * @author Stuart Douglas
 */
public class ApplicationClientStartService implements Service<ApplicationClientStartService> {


    public static final ServiceName SERVICE_NAME = ServiceName.of("appClientStart");

    private final Method mainMethod;
    private final ServiceName topLevelDeploymentName;
    private final String[] parameters;

    private Thread thread;

    private final Logger logger = Logger.getLogger(ApplicationClientStartService.class);

    public ApplicationClientStartService(final Method mainMethod, final ServiceName topLevelDeploymentName, final String[] parameters) {
        this.mainMethod = mainMethod;
        this.topLevelDeploymentName = topLevelDeploymentName;
        this.parameters = parameters;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final DeploymentCompleteListener listener = new DeploymentCompleteListener();
        final ServiceController<?> deployment = context.getController().getServiceContainer().getRequiredService(topLevelDeploymentName);
        deployment.addListener(ServiceListener.Inheritance.ALL, listener);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.waitForDeploymentStart();
                    mainMethod.invoke(null,new Object[] { parameters});
                } catch (InvocationTargetException e) {
                    logger.error(e);
                } catch (IllegalAccessException e) {
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


    private final class DeploymentCompleteListener extends AbstractServiceListener {

        private int outstanding = 0;

        @Override
        public synchronized void transition(final ServiceController serviceController, final ServiceController.Transition transition) {
            if (transition.entersRestState()) {
                outstanding--;
                if (outstanding == 0) {
                    notifyAll();
                }
            } else if (transition.leavesRestState()) {
                outstanding++;
            }
        }

        @Override
        public synchronized void listenerAdded(final ServiceController serviceController) {
            if (!serviceController.getSubstate().isRestState()) {
                outstanding++;
            }
        }

        public synchronized void waitForDeploymentStart() {
            while (outstanding > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
