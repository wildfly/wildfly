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

package org.jboss.as.deployment;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Service listener used by deployment to collect the result of all the services being started.
 *
 * @author John E. Bailey
 */
public class DeploymentServiceListener extends AbstractServiceListener<Object>{
    private volatile int batchCount;
    private volatile int totalServices;
    private final AtomicBoolean deploymentFinished = new AtomicBoolean();
    private final AtomicBoolean batchInProgress = new AtomicBoolean();
    private final long start = System.currentTimeMillis();
    private final Map<ServiceName, StartException> serviceFailures = new HashMap<ServiceName, StartException>();
    private final Callback deploymentCallback;
    private Runnable batchCompletionTask;

    private static final AtomicIntegerFieldUpdater<DeploymentServiceListener> countUpdater = AtomicIntegerFieldUpdater.newUpdater(DeploymentServiceListener.class, "batchCount");

    private static final AtomicIntegerFieldUpdater<DeploymentServiceListener> totalServicesUpdater = AtomicIntegerFieldUpdater.newUpdater(DeploymentServiceListener.class, "totalServices");

    /**
     * Construct new instance with a callback listener.
     * 
     * @param deploymentFinishCallback The finish callback
     */
    public DeploymentServiceListener(final Callback deploymentFinishCallback) {
        this.deploymentCallback = deploymentFinishCallback;
    }

    /** {@inheritDoc} */
    public void listenerAdded(final ServiceController<? extends Object> serviceController) {
        countUpdater.incrementAndGet(this);
        totalServicesUpdater.incrementAndGet(this);
    }

    /** {@inheritDoc} */
    public void serviceStarted(final ServiceController<? extends Object> serviceController) {
        if (countUpdater.decrementAndGet(this) == 0) {
            batchComplete();
        }
        serviceController.removeListener(this);
    }

    /** {@inheritDoc} */
    public void serviceFailed(ServiceController<? extends Object> serviceController, StartException reason) {
        final ServiceName serviceName = serviceController.getName();
        serviceFailures.put(serviceName, reason);
        if (countUpdater.decrementAndGet(this) == 0) {
            batchComplete();
        }
        serviceController.removeListener(this);
    }

    /**
     * Set the listener up to support a new batch.
     *
     * @throws DeploymentException if any problems occur while starting the batch.
     */
    public void startBatch() throws DeploymentException {
        startBatch(null);
    }

    /**
     * * Set the listener up to support a new batch with a finish task to be run when the batch of services completes.
     *
     * @param batchCompletionTask A task to execute when all services are started.
     * @throws DeploymentException if any problems occur while starting the batch.
     */
    public void startBatch(final Runnable batchCompletionTask) throws DeploymentException {
        if(deploymentFinished.get()) {
            throw new DeploymentException("Deployment has already been marked as finished.");
        }
        if(batchInProgress.compareAndSet(false, true) && countUpdater.compareAndSet(this, 0, 1)) {
            this.batchCompletionTask = batchCompletionTask;
        } else {
            throw new DeploymentException("Batch is already in progress");
        }
    }

    /**
     * Call when all services in this group have been added.
     */
    public void finishBatch() {
        if (countUpdater.decrementAndGet(this) == 0) {
            batchComplete();
        }
    }

    private void batchComplete() {
        if(batchInProgress.compareAndSet(true, false)) {
            if(batchCompletionTask != null)
                batchCompletionTask.run();
            if(!batchInProgress.get() && deploymentFinished.get()) {
                deploymentComplete();
            }
        }
    }

    /**
     * Mark the current deployment as finished.  
     */
    public void finishDeployment() {
        if (deploymentFinished.compareAndSet(false, true) && !batchInProgress.get()) {
            deploymentComplete();
        }
    }

    private void deploymentComplete() {
        final long end = System.currentTimeMillis();
        if(deploymentCallback != null) {
            deploymentCallback.run(serviceFailures, end - start, totalServices);
        }
    }

    public static interface Callback {
        void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int numberServices);
    }
}
