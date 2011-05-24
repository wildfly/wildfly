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

package org.jboss.as.controller;

import java.io.IOException;
import java.io.InputStream;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * The context for an operation step execution.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface NewOperationContext {

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(NewStepHandler step, Stage stage) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param response the response which the nested step should populate
     * @param operation the operation body to pass into the added step
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(ModelNode response, ModelNode operation, NewStepHandler step, Stage stage) throws IllegalArgumentException;

    /**
     * Get a stream which is attached to the request.
     *
     * @param index the index
     * @return the input stream
     * @throws IOException if an error occurs while opening the stream
     */
    InputStream getAttachmentStream(int index) throws IOException;

    /**
     * Get the node into which the operation result should be written.
     *
     * @return the result node
     */
    ModelNode getResult();

    /**
     * Complete a step, returning the overall operation result.  The step handler calling this operation should append
     * its result status to the operation result before calling this method.  The return value should be checked
     * to determine whether the operation step should be rolled back.
     *
     * @return the operation result action to take
     */
    ResultAction completeStep();

    /**
     * Get the failure description result node, creating it if necessary.
     *
     * @return the failure description
     */
    ModelNode getFailureDescription();

    /**
     * Get the operation context type.  This can be used to determine whether an operation is executing on a
     * server or on a host controller, etc.
     *
     * @return the operation context type
     */
    Type getType();

    /**
     * Determine whether the controller is currently performing boot tasks.
     *
     * @return whether the controller is currently booting
     */
    boolean isBooting();

    /**
     * Get the model node registration.  The registration is relative to the operation address.
     *
     * @return the model node registration
     */
    ModelNodeRegistration getModelNodeRegistration();

    /**
     * Get the service registry.  If the step is not a runtime operation handler step, an exception will be thrown.  The
     * returned registry must not be used to remove services.
     *
     * @param modify {@code true} if the operation may be modifying a service, {@code false} otherwise
     * @return the service registry
     * @throws UnsupportedOperationException if the calling step is not a runtime operation step
     */
    ServiceRegistry getServiceRegistry(boolean modify) throws UnsupportedOperationException;

    /**
     * Initiate a service removal.  If the step is not a runtime operation handler step, an exception will be thrown.  Any
     * subsequent step which attempts to add a service with the same name will block until the service removal completes.
     * The returned controller may be used to attempt to cancel a removal in progress.
     *
     * @param name the service to remove
     * @return the controller of the service to be removed
     * @throws UnsupportedOperationException if the calling step is not a runtime operation step
     */
    ServiceController<?> removeService(ServiceName name) throws UnsupportedOperationException;

    /**
     * Initiate a service removal.  If the step is not a runtime operation handler step, an exception will be thrown.  Any
     * subsequent step which attempts to add a service with the same name will block until the service removal completes.
     *
     * @param controller the service controller to remove
     * @throws UnsupportedOperationException if the calling step is not a runtime operation step
     */
    void removeService(ServiceController<?> controller) throws UnsupportedOperationException;

    /**
     * Get the service target.  If the step is not a runtime operation handler step, an exception will be thrown.  The
     * returned service target is limited such that only the service add methods are supported.  If a service added
     * to this target was removed by a prior operation step, the install will wait until the removal completes.
     *
     * @return the service target
     * @throws UnsupportedOperationException if the calling step is not a runtime operation step
     */
    ServiceTarget getServiceTarget() throws UnsupportedOperationException;

    /**
     * Read a model location, relative to the executed operation address.  Reads never block.  If a write action was
     * previously performed, the value read will be from an uncommitted copy of the the management model.  The returned
     * submodel is read-only.
     *
     * @param address the (possibly empty) address to read
     * @return the model data
     */
    ModelNode readModel(PathAddress address);

    /**
     * Read a model location, relative to the executed operation address, for the purpose of updating the submodel.
     * This is a write operation, and because only one operation
     * may write at a time, this operation may block until other writing operations have completed.
     *
     * @param address the (possibly empty) address to read
     * @return the model data
     */
    ModelNode readModelForUpdate(PathAddress address);

    /**
     * Get a read-only reference of the entire management model.  The structure of the returned model may depend
     * on the context type (domain vs. server).
     *
     * @return the read-only model
     */
    ModelNode getModel();

    /**
     * Write new content to a model location, relative to the executed operation address.  Since only one operation
     * may write at a time, this operation may block until other writing operations have completed.
     *
     * @param address the (possibly empty) address to write
     * @param newData the new value for the node
     * @return the old value of the node
     * @throws UnsupportedOperationException if the calling operation is not a model operation
     */
    ModelNode writeModel(PathAddress address, ModelNode newData) throws UnsupportedOperationException;

    /**
     * Remote a model node, relative to the executed operation address.  Since only one operation
     * may write at a time, this operation may block until other writing operations have completed.
     *
     * @param address the (possibly empty) address to remove
     * @return the old value of the node
     * @throws UnsupportedOperationException if the calling operation is not a model operation
     */
    ModelNode removeModel(PathAddress address) throws UnsupportedOperationException;

    /**
     * Determine whether the model has thus far been affected by this operation.
     *
     * @return {@code true} if the model was affected, {@code false} otherwise
     */
    boolean isModelAffected();

    /**
     * Determine whether the runtime container has thus far been affected by this operation.
     *
     * @return {@code true} if the container was affected, {@code false} otherwise
     */
    boolean isRuntimeAffected();

    /**
     * Get the current stage of execution.
     *
     * @return the current stage
     */
    Stage getCurrentStage();

    /**
     * Get the compensating operation node, creating it if necessary.
     *
     * @return the compensating operation node
     */
    ModelNode getCompensatingOperation();

    /**
     * Send a message to the client.  Valid only during this operation.
     *
     * @param severity the message severity
     * @param message the message
     */
    void report(MessageSeverity severity, String message);

    /**
     * The stage at which a step should apply.
     */
    enum Stage {
        /**
         * A pseudo-stage which will execute immediately after the current running step.
         */
        IMMEDIATE,
        /**
         * The step applies to the model (read or write).
         */
        MODEL,
        /**
         * The step applies to the runtime container (read or write).
         */
        RUNTIME,
        /**
         * The step checks the result of a runtime container operation (read only).  Inspect the container,
         * and if problems are detected, record the problem(s) in the operation result.
         */
        VERIFY,
        /**
         * The operation has completed execution.
         */
        DONE;

        Stage() {
        }

        Stage next() {
            switch (this) {
                case MODEL: return RUNTIME;
                case RUNTIME: return VERIFY;
                case VERIFY: return DONE;
                case DONE:
                default: throw new IllegalStateException();
            }
        }
    }

    /**
     * The type of controller this operation is being applied to.
     */
    enum Type {
        /**
         * A host controller with an active runtime.
         */
        HOST,
        /**
         * A running server instance with an active runtime container.
         */
        SERVER,
        /**
         * A server instance which is in management-only mode (no runtime container is available).
         */
        MANAGEMENT,
    }

    /**
     * The result action.
     */
    enum ResultAction {
        /**
         * The operation will be committed to the model and/or runtime.
         */
        KEEP,
        /**
         * The operation will be reverted.
         */
        ROLLBACK,
    }
}
