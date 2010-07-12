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

package org.jboss.as.deployment;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.TimingServiceListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Service listener used by deployment to allow a process to wait for a set of deployment services to be fully installed.
 * A call to {@code DeploymentServiceListener.waitForCompletion} will cause the current thread to wait until all services
 * are started or have failed to start.  It will throw an exception if any exceptions are thrown starting the services. 
 *
 * @author John E. Bailey
 */
public class DeploymentServiceListener implements ServiceListener {
    public static final long DEPLOYMENT_TIMEOUT_IN_SECONDS = Long.parseLong(System.getProperty("org.jboss.as.deployment.DeploymentTimeout", "30"));

    private final CountDownLatch latch = new CountDownLatch(1);
    private final TimingServiceListener delegateListener = new TimingServiceListener(new Runnable() {
        @Override
        public void run() {
            System.out.print("Finished");
            latch.countDown();
        }
    });
    private List<StartException> startExceptions = new ArrayList<StartException>();

    public void waitForCompletion() throws DeploymentException {
        delegateListener.finishBatch();
        try {
            latch.await(DEPLOYMENT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch(Exception e) {
            throw new DeploymentException("Failed to install deployment batch", e);
        }
        final boolean finished = delegateListener.finished();
        if(!finished) {
            throw new DeploymentException("Failed to execute deployment within timeout.");
        }
        if(finished && !startExceptions.isEmpty()) {
            throw new DeploymentException("Deployment failed to start " + startExceptions, startExceptions.get(0));
        }
    }

    @Override
    public void listenerAdded(ServiceController serviceController) {
        delegateListener.listenerAdded(serviceController);
    }

    @Override
    public void serviceStarting(ServiceController serviceController) {
        delegateListener.serviceStarting(serviceController);
    }

    @Override
    public void serviceStarted(ServiceController serviceController) {
        delegateListener.serviceStarted(serviceController);
    }

    @Override
    public void serviceFailed(ServiceController serviceController, StartException reason) {
        startExceptions.add(reason);
        delegateListener.serviceFailed(serviceController, reason);
    }

    @Override
    public void serviceStopping(ServiceController serviceController) {
        delegateListener.serviceStopping(serviceController);
    }

    @Override
    public void serviceStopped(ServiceController serviceController) {
        delegateListener.serviceStopped(serviceController);
    }

    @Override
    public void serviceRemoved(ServiceController serviceController) {
        delegateListener.serviceRemoved(serviceController);
    }
}
