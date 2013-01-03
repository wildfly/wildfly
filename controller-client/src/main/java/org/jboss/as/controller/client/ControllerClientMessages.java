/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client;

import org.jboss.as.controller.client.helpers.domain.DeploymentAction.Type;
import org.jboss.as.controller.client.helpers.domain.RollbackCancelledException;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;

import java.io.IOException;
import java.net.URL;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ControllerClientMessages {

    /**
     * The messages
     */
    ControllerClientMessages MESSAGES = Messages.getBundle(ControllerClientMessages.class);

    // 10620 - 10699

    /**
     * Creates an exception indicating after starting creation of the rollout plan no deployment actions can be added.
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 10620, value = "Cannot add deployment actions after starting creation of a rollout plan")
    IllegalStateException cannotAddDeploymentAction();

    /**
     * Creates an exception indicating no deployment actions can be added after starting the creation of the rollout
     * plan.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10621, value = "Cannot add deployment actions after starting creation of a rollout plan")
    IllegalStateException cannotAddDeploymentActionsAfterStart();

    /**
     * A message indicating that {@code first} cannot be converted to {@code second}.
     *
     * @param first  the type that could not be converted.
     * @param second the type attempting to be converted to.
     *
     * @return the message.
     */
    @Message(id = 10622, value = "Cannot convert %s to %s")
    String cannotConvert(String first, String second);

    /**
     * Creates an exception indicating the deployment name could not be derived from the URL.
     *
     * @param url the URL to the deployment.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10623, value = "Cannot derive a deployment name from %s -- use an overloaded method variant that takes a 'name' parameter")
    IllegalArgumentException cannotDeriveDeploymentName(URL url);

    /**
     * Creates an exception indicating the {@code DeploymentPlan} cannot be used because it was not created by this
     * manager.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10624, value = "Cannot use a DeploymentPlan not created by this manager")
    IllegalArgumentException cannotUseDeploymentPlan();

    /**
     * Creates an exception indicating the channel is closed.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 10625, value = "Channel closed")
    IOException channelClosed(@Cause IOException cause);

    /**
     * A message indicating the a deployment with the {@code name} is already present in the domain.
     *
     * @param name the name of the deployment.
     *
     * @return the message.
     */
    @Message(id = 10626, value = "Deployment with name %s already present in the domain")
    String domainDeploymentAlreadyExists(String name);

    /**
     * The word failed.
     *
     * @return failed.
     */
    @Message(id = 10627, value = "failed")
    String failed();

    /**
     * Creates an exception indicating a global rollback is not compatible with a server restart.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10628, value = "Global rollback is not compatible with a server restart")
    IllegalStateException globalRollbackNotCompatible();

    /**
     * Creates an exception indicating the graceful shutdown already configured with a timeout, represented by the
     * {@code timeout} parameter.
     *
     * @param timeout the already configured timeout.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10629, value = "Graceful shutdown already configured with a timeout of %d ms")
    IllegalStateException gracefulShutdownAlreadyConfigured(long timeout);

    /**
     * A message indicating only one version of a deployment with a given unique name can exist in the domain.
     *
     * @param deploymentName the deployment name.
     * @param missingGroups  the missing groups.
     *
     * @return the message.
     */
    @Message(id = 10630, value = "Only one version of deployment with a given unique name can exist in the domain. The deployment " +
            "plan specified that a new version of deployment %s replace an existing deployment with the same unique " +
            "name, but did not apply the replacement to all server groups. Missing server groups were: %s")
    String incompleteDeploymentReplace(String deploymentName, String missingGroups);

    /**
     * Creates an exception indicating the action type, represented by the {@code type} parameter, is invalid.
     *
     * @param type the invalid type.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10631, value = "Invalid action type %s")
    IllegalStateException invalidActionType(Type type);

    /**
     * Creates an exception indicating the preceding action was not a
     * {@link org.jboss.as.controller.client.helpers.standalone.DeploymentAction.Type type}.
     *
     * @param type the type that preceding action should be.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10632, value = "Preceding action was not a %s")
    IllegalStateException invalidPrecedingAction(Object type);

    /**
     * Creates an exception indicating the URL is not a valid URI.
     *
     * @param cause the cause of the error.
     * @param url   the URL.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10633, value = "%s is not a valid URI")
    IllegalArgumentException invalidUri(@Cause Throwable cause, URL url);

    /**
     * Creates an exception indicating the value is invalid and must be greater than the {@code minValue}.
     *
     * @param name     the name for the value.
     * @param value    the invalid value.
     * @param minValue the minimum value allowed.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10634, value = "Illegal %s value %d -- must be greater than %d")
    IllegalArgumentException invalidValue(String name, int value, int minValue);

    /**
     * Creates an exception indicating the value is invalid and must be greater than the {@code minValue} and less than
     * the {@code maxValue}.
     *
     * @param name     the name for the value.
     * @param value    the invalid value.
     * @param minValue the minimum value allowed.
     * @param maxValue the maximum value allowed
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10635, value = "Illegal %s value %d -- must be greater than %d and less than %d")
    IllegalArgumentException invalidValue(String name, int value, int minValue, int maxValue);

    /**
     * Creates an exception indicating that screen real estate is expensive and displayUnits must be 5 characters or
     * less.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10636, value = "Screen real estate is expensive; displayUnits must be 5 characters or less")
    RuntimeException maxDisplayUnitLength();

    /**
     * Creates an exception indicating no active request found for the batch id.
     *
     * @param batchId the batch id.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 10637, value = "No active request found for %d")
    IOException noActiveRequest(int batchId);

    /**
     * A message indicating that no failure details were provided.
     *
     * @return the message.
     */
    @Message(id = 10638, value = "No failure details provided")
    String noFailureDetails();

    /**
     * Creates an exception indicating the {@code name} is not configured.
     *
     * @param name the name that is not configured.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10639, value = "No %s is configured")
    IllegalStateException notConfigured(String name);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10640, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating the object is closed.
     *
     * @param name the name of the object.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10641, value = "%s is closed")
    IllegalStateException objectIsClosed(String name);

    /**
     * Creates an exception with the operation outcome.
     *
     * @param outcome the operation outcome.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10642, value = "Operation outcome is %s")
    RuntimeException operationOutcome(String outcome);

    /**
     * Creates an exception indicating operations are not not allowed after content and deployment modifications.
     *
     * @param name the name for the operations.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10643, value = "%s operations are not allowed after content and deployment modifications")
    IllegalStateException operationsNotAllowed(String name);

    /**
     * Creates an exception indicating the rollback was cancelled.
     *
     * @return a {@link RollbackCancelledException} for the error.
     */
    @Message(id = 10644, value = "Rollback was cancelled")
    RollbackCancelledException rollbackCancelled();

    /**
     * Creates an exception indicating the rollback was itself rolled back.
     *
     * @return a {@link RollbackCancelledException} for the error.
     */
    @Message(id = 10645, value = "Rollback was itself rolled back")
    RollbackCancelledException rollbackRolledBack();

    /**
     * Creates an exception indicating the rollback timed out.
     *
     * @return a {@link RollbackCancelledException} for the error.
     */
    @Message(id = 10646, value = "Rollback timed out")
    RollbackCancelledException rollbackTimedOut();

    /**
     * A message indicating the a deployment with the {@code name} is already present in the domain.
     *
     * @param name the name of the deployment.
     *
     * @return the message.
     */
    @Message(id = 10647, value = "Deployment with name %s already present in the server")
    String serverDeploymentAlreadyExists(String name);

    /**
     * Creates an exception indicating the action type, represented by the {@code type} parameter, is unknown.
     *
     * @param type the unknown type.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10648, value = "Unknown action type %s")
    IllegalStateException unknownActionType(Object type);

    /**
     * Creates a leak description, used in the controller client to show the original allocation point creating the
     * client.
     *
     * @return the leak description
     */
    @Message(id = 10649, value = "Allocation stack trace:")
    LeakDescription controllerClientNotClosed();

    class LeakDescription extends Throwable {
        private static final long serialVersionUID = -7193498784746897578L;

        public LeakDescription() {
            //
        }

        public LeakDescription(String message) {
            super(message);
        }

        @Override
        public String toString() {
            // skip the class-name
            return getLocalizedMessage();
        }
    }

}
