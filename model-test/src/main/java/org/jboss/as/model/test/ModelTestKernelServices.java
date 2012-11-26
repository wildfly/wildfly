/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.model.test;

import java.io.InputStream;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface ModelTestKernelServices<T extends ModelTestKernelServices<T>> {

    /**
     * Get whether the controller booted successfully
     * @return true if the controller booted successfully
     */
    boolean isSuccessfulBoot();

    /**
     * Get any errors thrown on boot
     * @return the boot error
     */
    Throwable getBootError();

    /**
     * Gets the legacy controller services for the controller containing the passed in model version
     *
     * @param modelVersion the model version of the legacy model controller
     * @throws IllegalStateException if this is not the test's main model controller
     * @throws IllegalStateException if there is no legacy controller containing the version
     */
    T getLegacyServices(ModelVersion modelVersion);

    /**
     * Transforms an operation in the main controller to the format expected by the model controller containing
     * the legacy subsystem
     *
     * @param modelVersion the subsystem model version of the legacy subsystem model controller
     * @param operation the operation to transform
     * @return the transformed operation
     * @throws IllegalStateException if this is not the test's main model controller
     */
    TransformedOperation transformOperation(ModelVersion modelVersion, ModelNode operation) throws OperationFailedException;

    /**
     * Transforms the model to the legacy subsystem model version
     * @param modelVersion the target legacy subsystem model version
     * @return the transformed model
     * @throws IllegalStateException if this is not the test's main model controller
     */
    ModelNode readTransformedModel(ModelVersion modelVersion);

    /**
     * Execute an operation in the  controller containg the passed in version of the subsystem.
     * The operation and results will be translated from the format for the main controller to the
     * legacy controller's format.
     *
     * @param modelVersion the subsystem model version of the legacy subsystem model controller
     * @param op the operation for the main controller
     * @throws IllegalStateException if this is not the test's main model controller
     * @throws IllegalStateException if there is no legacy controller containing the version of the subsystem
     */
    ModelNode executeOperation(final ModelVersion modelVersion, final TransformedOperation op);

    /**
     * Reads the whole model from the model controller without aliases or runtime attributes/resources
     *
     * @return the whole model
     */
    ModelNode readWholeModel();

    /**
     * Reads the whole model from the model controller without runtime attributes/resources
     *
     * @param includeAliases whether to include aliases
     * @return the whole model
     */
    ModelNode readWholeModel(boolean includeAliases);

    /**
     * Reads the whole model from the model controller
     *
     * @param includeAliases whether to include aliases
     * @param includeRuntime whether to include runtime attributes/resources
     * @return the whole model
     */
    ModelNode readWholeModel(boolean includeAliases, boolean includeRuntime);

    /**
     * Gets the service container
     *
     * @return the service container
     */
    ServiceContainer getContainer();

    /**
     * Execute an operation in the model controller
     *
     * @param operation the operation to execute
     * @param inputStream Input Streams for the operation
     * @return the whole result of the operation
     */
    ModelNode executeOperation(ModelNode operation, InputStream... inputStreams);

    ModelNode executeOperation(ModelNode operation, OperationTransactionControl txControl);

    ModelNode executeForResult(ModelNode operation, InputStream... inputStreams) throws OperationFailedException;

    /**
     * Execute an operation in the model controller, expecting failure.
     * Gives a junit {@link AssertionFailedError} if the operation did not fail.
     *
     * @param operation the operation to execute
     * @return the result of the operation
     */
    void executeForFailure(ModelNode operation, InputStream... inputStreams);

    /**
     * Reads the persisted subsystem xml
     *
     * @return the xml
     */
    String getPersistedSubsystemXml();

    /**
     * Validates the operations against the description providers in the model controller
     *
     * @param operations the operations to validate
     * @throws AssertionFailedError if the operations are not valid
     */
    void validateOperations(List<ModelNode> operations);

    /**
     * Validates the operation against the description providers in the model controller
     *
     * @param operation the operation to validate
     * @throws AssertionFailedError if the operation is not valid
     */
    void validateOperation(ModelNode operation);

    void shutdown();

    ImmutableManagementResourceRegistration getRootRegistration();

}