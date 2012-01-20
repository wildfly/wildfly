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

package org.jboss.as.domain.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;

import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 10800-10899. This file is using the subset 10830-10899 for domain
 * controller non-logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 * <p/>
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface DomainControllerMessages {

    /**
     * The messages.
     */
    DomainControllerMessages MESSAGES = Messages.getBundle(DomainControllerMessages.class);

    /**
     * Creates an exception message indicating this host is a slave and cannot accept registrations from other slaves.
     *
     * @return a message for the error.
     */
    @Message(id = 10830, value = "Registration of remote hosts is not supported on slave host controllers")
    String slaveControllerCannotAcceptOtherSlaves();

    /**
     * Creates an exception message indicating this host is in admin mode and cannot accept registrations from other
     * slaves.
     *
     * @param runningMode the host controller's current running mode
     *
     * @return a message for the error.
     */
    @Message(id = 10831, value = "The master host controller cannot register slave host controllers as it's current running mode is '%s'")
    String adminOnlyModeCannotAcceptSlaves(RunningMode runningMode);

    /**
     * Creates an exception message indicating a host cannot register because another host of the same name is already
     * registered.
     *
     * @param slaveName the name of the slave
     *
     * @return a message for the error.
     */
    @Message(id = 10832, value = "There is already a registered host named '%s'")
    String slaveAlreadyRegistered(String slaveName);

    /**
     * Creates an exception message indicating that a parent is missing a required child.
     *
     * @param parent     the name of the parent element
     * @param child      the name of the missing child element
     * @param parentSpec the complete string representation of the parent element
     *
     * @return the error message
     */
    @Message(id = 10833, value = "%s is missing %s: %s")
    String requiredChildIsMissing(String parent, String child, String parentSpec);

    /**
     * Creates an exception message indicating that a parent recognizes only the specified children.
     *
     * @param parent     the name of the parent element
     * @param children   recognized children
     * @param parentSpec the complete string representation of the parent element
     *
     * @return the error message
     */
    @Message(id = 10834, value = "%s recognizes only %s as children: %s")
    String unrecognizedChildren(String parent, String children, String parentSpec);

    /**
     * Creates an exception message indicating that in-series is missing groups.
     *
     * @param rolloutPlan string representation of a rollout plan
     *
     * @return the error message
     */
    @Message(id = 10835, value = IN_SERIES + " is missing groups: %s")
    String inSeriesIsMissingGroups(String rolloutPlan);

    /**
     * Creates an exception message indicating that server-group expects one and only one child.
     *
     * @param rolloutPlan string representation of a rollout plan
     *
     * @return the error message
     */
    @Message(id = 10836, value = SERVER_GROUP + " expects one and only one child: %s")
    String serverGroupExpectsSingleChild(String rolloutPlan);

    /**
     * Creates an exception message indicating that one of the groups in rollout plan does not define neither
     * server-group nor concurrent-groups.
     *
     * @param rolloutPlan string representation of a rollout plan
     *
     * @return the error message
     */
    @Message(id = 10837, value = "One of the groups does not define neither " + SERVER_GROUP + " nor " + CONCURRENT_GROUPS + ": %s")
    String unexpectedInSeriesGroup(String rolloutPlan);

    /**
     * A message indicating an unexplained failure.
     *
     * @return the message.
     */
    @Message(id = 10838, value = "Unexplained failure")
    String unexplainedFailure();

    /**
     * A message indicating the operation failed or was rolled back on all servers.
     *
     * @return the message.
     */
    @Message(id = 10839, value = "Operation failed or was rolled back on all servers.")
    String operationFailedOrRolledBack();

    /**
     * A message indicating an interruption waiting for the result from the server.
     *
     * @param server the server.
     *
     * @return the message.
     */
    @Message(id = 10840, value = "Interrupted waiting for result from server %s")
    String interruptedAwaitingResultFromServer(ServerIdentity server);

    /**
     * A message indicating an exception occurred getting the result from the server.
     *
     * @param server  the server.
     * @param message the error message.
     *
     * @return the message.
     */
    @Message(id = 10841, value = "Exception getting result from server %s: %s")
    String exceptionAwaitingResultFromServer(ServerIdentity server, String message);

    /**
     * A message indicating an invalid rollout plan. The {@code modelNode} is not a valid child of the node represented
     * by the {@code nodeName} parameter.
     *
     * @param modelNode the model node.
     * @param nodeName  the name of the node.
     *
     * @return the message.
     */
    @Message(id = 10842, value = "Invalid rollout plan. %s is not a valid child of node %s")
    String invalidRolloutPlan(ModelNode modelNode, String nodeName);

    /**
     * A message indicating an invalid rollout plan. The plan operations affect server the server groups represented by
     * the server {@code groups} parameter that are not reflected in the rollout plan.
     *
     * @param groups the server groups that are not reflected in the rollout plan.
     *
     * @return the message.
     */
    @Message(id = 10843, value = "Invalid rollout plan. Plan operations affect server groups %s that are not reflected in the rollout plan")
    String invalidRolloutPlan(Set<String> groups);

    /**
     * A message indicating an invalid rollout plan. The server group, represented by the {@code group} parameter,
     * appears more than once in the plan.
     *
     * @param group the server group that appears more than once.
     *
     * @return the message.
     */
    @Message(id = 10844, value = "Invalid rollout plan. Server group %s appears more than once in the plan.")
    String invalidRolloutPlanGroupAlreadyExists(String group);

    /**
     * A message indicating an invalid rollout plan. The server group, represented by the {@code name} parameter, has an
     * invalid value and must be between 0 and 100.
     *
     * @param name         the name of the group.
     * @param propertyName the name of the property.
     * @param value        the invalid value.
     *
     * @return the message.
     */
    @Message(id = 10845, value = "Invalid rollout plan. Server group %s has a %s value of %s; must be between 0 and 100.")
    String invalidRolloutPlanRange(String name, String propertyName, int value);

    /**
     * A message indicating an invalid rollout plan. The server group, represented by the {@code name} parameter, has an
     * invalid value and cannot be less than 0.
     *
     * @param name         the name of the group.
     * @param propertyName the name of the property.
     * @param value        the invalid value.
     *
     * @return the message.
     */
    @Message(id = 10846, value = "Invalid rollout plan. Server group %s has a %s value of %s; cannot be less than 0.")
    String invalidRolloutPlanLess(String name, String propertyName, int value);

    /**
     * A message indicating an interruption waiting for the result from host.
     *
     * @param name the name of the host.
     *
     * @return the message.
     */
    @Message(id = 10847, value = "Interrupted waiting for result from host %s")
    String interruptedAwaitingResultFromHost(String name);

    /**
     * A message indicating an exception occurred getting the result from the host.
     *
     * @param name    the name of the host.
     * @param message the error message.
     *
     * @return the message.
     */
    @Message(id = 10848, value = "Exception getting result from host %s: %s")
    String exceptionAwaitingResultFromHost(String name, String message);

    /**
     * A message indicating the operation, represented by the {@code operation} parameter, for the {@code address} can
     * only be handled by the master domain controller and this host is not the master domain controller.
     *
     * @param operation the operation.
     * @param address   the address the operation was to be executed on.
     *
     * @return the message.
     */
    @Message(id = 10849, value = "Operation %s for address %s can only be handled by the " +
            "master Domain Controller; this host is not the master Domain Controller")
    String masterDomainControllerOnlyOperation(String operation, PathAddress address);

    /**
     * A message indicating there is no handler for the operation at the address.
     *
     * @param operationName the operation name
     * @param address       the address the operation was to be executed on.
     *
     * @return the message.
     */
    @Message(id = 10850, value = "No handler for operation %s at address %s")
    String noHandlerForOperation(String operationName, PathAddress address);

    /**
     * A message indicating the operation targets host, but the host is not registered.
     *
     * @param name the name of the host.
     *
     * @return the message.
     */
    @Message(id = 10851, value = "Operation targets host %s but that host is not registered")
    String invalidOperationTargetHost(String name);

    /**
     * A message indicating an exception was caught storing the deployment content.
     *
     * @param exceptionName the name of the caught exception.
     * @param exception     the exception.
     *
     * @return the message.
     */
    @Message(id = 10852, value = "Caught %s storing deployment content -- %s")
    String caughtExceptionStoringDeploymentContent(String exceptionName, Throwable exception);

    /**
     * Creates an exception indicating an unexpected initial path key.
     *
     * @param key the unexpected key.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10853, value = "Unexpected initial path key %s")
    IllegalStateException unexpectedInitialPathKey(String key);

    /**
     * A message indicating the stream is {@code null} at the index.
     *
     * @param index the index.
     *
     * @return the message.
     */
    @Message(id = 10854, value = "Null stream at index %d")
    String nullStream(int index);

    /**
     * A message indicating the byte stream is invalid.
     *
     * @return the message.
     */
    @Message(id = 10855, value = "Invalid byte stream.")
    String invalidByteStream();

    /**
     * A message indicating the url stream is invalid.
     *
     * @return the message.
     */
    @Message(id = 10856, value = "Invalid url stream.")
    String invalidUrlStream();

    /**
     * A message indicating that only 1 piece of content is currently supported. See JBAS-9020.
     *
     * @return the message.
     */
    @Message(id = 10857, value = "Only 1 piece of content is currently supported (AS7-431)")
    String as7431();

    /**
     * A message indicating no deployment content with the hash is available in the deployment content repository.
     *
     * @param hash the hash.
     *
     * @return the message.
     */
    @Message(id = 10858, value = "No deployment content with hash %s is available in the deployment content repository.")
    String noDeploymentContentWithHash(String hash);

    /**
     * A message indicating a slave domain controller cannot accept deployment content uploads.
     *
     * @return the message.
     */
    @Message(id = 10859, value = "A slave domain controller cannot accept deployment content uploads")
    String slaveCannotAcceptUploads();

    /**
     * A message indicating no deployment content with the name was found.
     *
     * @param name the name of the deployment.
     *
     * @return the message.
     */
    @Message(id = 10860, value = "No deployment with name %s found")
    String noDeploymentContentWithName(String name);

    /**
     * A message indicating the deployment cannot be removed from the domain as it is still used by the server groups.
     *
     * @param name   the name of the deployment.
     * @param groups the server groups using the deployment.
     *
     * @return the message.
     */
    @Message(id = 10861, value = "Cannot remove deployment %s from the domain as it is still used by server groups %s")
    String cannotRemoveDeploymentInUse(String name, List<String> groups);

    /**
     * A message indicating the {@code name} has an invalid value.
     *
     * @param name     the name of the attribute.
     * @param value    the invalid value.
     * @param maxIndex the maximum index.
     *
     * @return the message.
     */
    @Message(id = 10862, value = "Invalid '%s' value: %d, the maximum index is %d")
    String invalidValue(String name, int value, int maxIndex);

    /**
     * A message indicating the url is not valid.
     *
     * @param url     the invalid url.
     * @param message an error message.
     *
     * @return the message.
     */
    @Message(id = 10863, value = "%s is not a valid URL -- %s")
    String invalidUrl(String url, String message);

    /**
     * A message indicating an error occurred obtaining the input stream from the URL.
     *
     * @param url     the invalid url.
     * @param message an error message.
     *
     * @return the message.
     */
    @Message(id = 10864, value = "Error obtaining input stream from URL %s -- %s")
    String errorObtainingUrlStream(String url, String message);

    /**
     * A message indicating an invalid content declaration.
     *
     * @return the message.
     */
    @Message(id = 10865, value = "Invalid content declaration")
    String invalidContentDeclaration();

    /**
     * Creates an exception indicating the variable is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10866, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * A message indicating the operation, represented by the {@code opName} parameter, cannot be used with the same
     * value for the parameters represented by {@code param1} and {@code param2}.
     *
     * @param opName         the operation name.
     * @param param1         the first parameter.
     * @param param2         the second parameter.
     * @param redeployOpName the redeploy operation name.
     * @param replaceOpName  the replace operation name.
     *
     * @return the message.
     */
    @Message(id = 10867, value = "Cannot use %s with the same value for parameters %s and %s. " +
            "Use %s to redeploy the same content or %s to replace content with a new version with the same name.")
    String cannotUseSameValueForParameters(String opName, String param1, String param2, String redeployOpName, String replaceOpName);

    /**
     * A message indicating the deployment is already started.
     *
     * @param name the name of the deployment.
     *
     * @return the message.
     */
    @Message(id = 10868, value = "Deployment %s is already started")
    String deploymentAlreadyStarted(String name);

    /**
     * A message indicating the {@code value} for the {@code name} is unknown.
     *
     * @param name  the name.
     * @param value the value.
     *
     * @return the message.
     */
    @Message(id = 10869, value = "Unknown %s %s")
    String unknown(String name, String value);

    /**
     * Creates an exception indicating the server group is unknown.
     *
     * @param serverGroup the unknown server group.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10870, value = "Unknown server group %s")
    IllegalStateException unknownServerGroup(String serverGroup);

    /**
     * Creates an exception indicating the server is unknown.
     *
     * @param server the unknown serve.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10871, value = "Unknown server %s")
    IllegalStateException unknownServer(ServerIdentity server);

    /**
     * Creates an exception indicating the code is invalid.
     *
     * @param code the invalid code.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10872, value = "Invalid code %d")
    IllegalArgumentException invalidCode(int code);

    /**
     * Creates an exception indicating the hash does not refer to any deployment.
     *
     * @param hash the invalid hash.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15873, value = "Repository does not contain any deployment with hash %s")
    IllegalStateException deploymentHashNotFoundInRepository(String hash);

    /**
     * Creates an exception indicating the hash does not refer to any deployment.
     *
     * @param hash the invalid hash.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15874, value = "Expected only one deployment, found %d")
    IllegalStateException expectedOnlyOneDeployment(int i);

    /**
     * Creates an exception indicating no profile could be found with the given name
     *
     * @param profile the profile name
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 10875, value = "No profile called: %s")
    OperationFailedException noProfileCalled(String profile);

    /**
     * Creates an exception indicating that a write-attribute call passed
     * in the exisiting value
     *
     * @param attributeValue the attribute value
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 10874, value = "Atribute value was the same as the exisiting one: %s")
    OperationFailedException writeAttributeNotChanged(String attributeValue);

}
