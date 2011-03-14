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

package org.jboss.as.controller.client.helpers.domain;

import java.util.Map;
import java.util.UUID;


/**
 * Describes the results of executing a {@link DeploymentAction} across
 * a domain.
 *
 * @author Brian Stansberry
 */
public interface DeploymentActionResult {

    /**
     * Gets the unique id of the deployment action.
     *
     * @return the id. Will not be <code>null</code>
     */
    UUID getDeploymentActionId();

    /**
     * Gets the deployment action that lead to this result.
     *
     * @return the action. Will not be <code>null</code>
     */
    DeploymentAction getDeploymentAction();

    /**
     * Gets whether the action was cancelled by the domain controller before
     * being applied to any servers.
     *
     * @return <code>true</code> if the action was cancelled; <code>false</code>
     *         otherwise
     */
    boolean isCancelledByDomain();

    /**
     * Gets whether the action was rolled back across the domain after being
     * successfully applied on the domain controller and all host controllers. Note that
     * depending on the configuration of the deployment plan, an action
     * can be rolled back on individual servers without triggering a rollback
     * across the domain.
     * <p>
     * This method will return <code>true</code> if a rollback was attempted,
     * whether or not it succeeded. See {@link #getDomainControllerRollbackFailure()}
     * and {@link #getHostControllerRollbackFailures()} to see if their were
     * failures on the domain controller or host controllers; for each server
     * see the {@link ServerUpdateResult} to see if there were failures
     * on the servers.
     * </p>
     *
     * @return <code>true</code> if rollback of the action was attempted; <code>false</code>
     *         otherwise
     */
    boolean isRolledBackOnDomain();

    /**
     * Gets any exception that occurred when applying this update on the
     * domain controller.
     *
     * @return the exception, or <code>null</code>
     */
    UpdateFailedException getDomainControllerFailure();

    /**
     * Gets any exception that occurred when rolling back this update on the
     * domain controller.
     *
     * @return the exception, or <code>null</code>
     */
    UpdateFailedException getDomainControllerRollbackFailure();

    /**
     * Gets any exceptions that occurred when applying this update on the
     * host controllers.
     *
     * @return the exceptions, keyed by the name of the host whose
     *          host controller threw the exception. Will not be <code>null</code>
     */
    Map<String, UpdateFailedException> getHostControllerFailures();

    /**
     * Gets any exceptions that occurred when rolling back this update on the
     * host controllers.
     *
     * @return the exceptions, keyed by the name of the host whose
     *          host controller threw the exception. Will not be <code>null</code>
     */
    Map<String, UpdateFailedException> getHostControllerRollbackFailures();

    /**
     * Gets the results of this action for each server group.
     *
     * @return map of server group results, keyed by server group name
     */
    Map<String, ServerGroupDeploymentActionResult> getResultsByServerGroup();
}
