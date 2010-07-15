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

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.TimingServiceListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Service listener used by deployment to collect the result of all the services being started.
 *
 * @author John E. Bailey
 */
public class DeploymentServiceListener implements ServiceListener {

    private final Map<ServiceName, StartException> serviceFailures = new HashMap<ServiceName, StartException>();
    private final TimingServiceListener delegateListener;

    public DeploymentServiceListener(final Callback callback) {
        delegateListener = new TimingServiceListener(new Runnable() {
            @Override
            public void run() {
                callback.run(serviceFailures, delegateListener.getElapsedTime(), delegateListener.getTotalCount());
            }
        });
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
        final ServiceName serviceName = serviceController.getName();
        serviceFailures.put(serviceName, reason);
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

    public int getTotalCount() {
        return delegateListener.getTotalCount();
    }

    public void finishBatch() {
        delegateListener.finishBatch();
    }

    public static interface Callback {
        void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int numberServices);
    }
}
