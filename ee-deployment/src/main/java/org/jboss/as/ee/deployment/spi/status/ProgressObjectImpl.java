/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ee.deployment.spi.status;

import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.ee.deployment.spi.DeploymentLogger.ROOT_LOGGER;
import static org.jboss.as.ee.deployment.spi.DeploymentMessages.MESSAGES;

/**
 * The ProgressObject interface tracks and reports the progress of the deployment activities, distribute, start, stop, undeploy.
 *
 * @author Thomas.Diesler@jboss.com
 *
 */
public class ProgressObjectImpl implements ProgressObject {

    // list of ProgressListener objects
    private List listeners = new ArrayList();

    private final DeploymentStatusImpl deploymentStatus;
    private final TargetModuleID[] targetModules;
    private final List<String> moduleIDs = new ArrayList<String>();

    public ProgressObjectImpl(DeploymentStatus deploymentStatus, TargetModuleID[] targetModules) {
        if (deploymentStatus == null)
            throw new IllegalArgumentException(MESSAGES.nullArgument("deploymentStatus"));
        if (targetModules == null)
            throw new IllegalArgumentException(MESSAGES.nullArgument("targetModules"));
        this.deploymentStatus = (DeploymentStatusImpl) deploymentStatus;
        this.targetModules = targetModules;
        for(TargetModuleID modid : targetModules) {
            moduleIDs.add(modid.getModuleID());
        }
    }

    /**
     * Set the current deployment status
     */
    public void sendProgressEvent(StateType stateType, String message, TargetModuleID moduleID) {
        ROOT_LOGGER.tracef("sendProgressEvent, state: %s, msg: %s", stateType, message);
        deploymentStatus.setStateType(stateType);
        deploymentStatus.setMessage(message);
        ProgressEvent progressEvent = new ProgressEvent(this, moduleID, deploymentStatus);
        for (int i = 0; i < listeners.size(); i++) {
            ProgressListener progressListener = (ProgressListener) listeners.get(i);
            progressListener.handleProgressEvent(progressEvent);
        }
    }

    /**
     * Retrieve the status of the deployment
     *
     * @return the status
     */
    public DeploymentStatus getDeploymentStatus() {
        return deploymentStatus;
    }

    /**
     * Retrieve the resulting target module ids
     *
     * @return the module ids
     */
    public TargetModuleID[] getResultTargetModuleIDs() {
        return targetModules;
    }

    /**
     * Return the client configuration associated with the module
     *
     * @param id the module id
     * @return the client configuration or null if none exists
     */
    public ClientConfiguration getClientConfiguration(TargetModuleID id) {
        return null; // [todo] implement method
    }

    /**
     * Is cancel supported
     *
     * @return true when cancel is supported, false otherwise
     */
    public boolean isCancelSupported() {
        return false;
    }

    /**
     * Cancels the deployment
     *
     * @throws javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException when cancel is not supported
     */
    public void cancel() throws OperationUnsupportedException {
        throw new OperationUnsupportedException("cancel");
    }

    /**
     * Is stop supported
     *
     * @return true when stop is supported, false otherwise
     */
    public boolean isStopSupported() {
        return false;
    }

    /**
     * Stops the deployment
     *
     * @throws javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException when stop is not supported
     */
    public void stop() throws OperationUnsupportedException {
        throw new OperationUnsupportedException("stop");
    }

    /**
     * Add a progress listener
     *
     * @param listener the listener
     */
    public void addProgressListener(ProgressListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a progress listener
     *
     * @param listener the listener
     */
    public void removeProgressListener(ProgressListener listener) {
        listeners.remove(listener);
    }

    @Override
    public String toString() {
        return "ProgressObject[status=" + deploymentStatus + ",modules=" + moduleIDs + "]";
    }
}
