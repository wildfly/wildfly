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

import javax.enterprise.deploy.shared.ActionType;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.status.DeploymentStatus;

/**
 * The DeploymentStatus interface provides information about the progress status of a deployment action.
 *
 * @author Thomas.Diesler@jboss.com
 *
 */
public class DeploymentStatusImpl implements DeploymentStatus {

    private StateType stateType;
    private CommandType commandType;
    private ActionType actionType;
    private String message;

    public DeploymentStatusImpl(StateType stateType, CommandType commandType, ActionType actionType, String message) {
        this.stateType = stateType;
        this.commandType = commandType;
        this.actionType = actionType;
        this.message = message;
    }

    /**
     * Set the current deployment status
     */
    void setStateType(StateType stateType) {
        this.stateType = stateType;
    }

    /**
     * Set the current deployment message
     */
    void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the state of the deployment
     *
     * @return the state
     */
    public StateType getState() {
        return stateType;
    }

    /**
     * The deployment command
     *
     * @return the command
     */
    public CommandType getCommand() {
        return commandType;
    }

    /**
     * The action of this deployment
     *
     * @return the action
     */
    public ActionType getAction() {
        return actionType;
    }

    /**
     * Get the message
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Is the deployment complete
     *
     * @return true when complete, false otherwise
     */
    public boolean isCompleted() {
        return stateType == StateType.COMPLETED;
    }

    /**
     * Has the deployment failed
     *
     * @return true when failed, false otherwise
     */
    public boolean isFailed() {
        return stateType == StateType.FAILED;
    }

    /**
     * Is the deployment in progress
     *
     * @return true when in progress, false otherwise
     */
    public boolean isRunning() {
        return stateType == StateType.RUNNING;
    }

    @Override
    public String toString() {
        return "DeploymentStatus[state=" + stateType +",command=" + commandType + ",action=" + actionType + ",message='" + message + "']";
    }
}
