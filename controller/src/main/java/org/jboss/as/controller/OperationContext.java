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

import java.io.InputStream;

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * The context for an operation step execution.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface OperationContext {

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(OperationStepHandler step, Stage stage) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @param addFirst add the handler before the others
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process, writing any output to the response object
     * associated with the current step.
     * Runtime operation steps are automatically added after configuration operation steps.  Since only one operation
     * may perform runtime work at a time, this method may block until other runtime operations have completed.
     *
     * @param operation the operation body to pass into the added step
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(final ModelNode operation, final OperationStepHandler step, final Stage stage) throws IllegalArgumentException;

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
    void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param response the response which the nested step should populate
     * @param operation the operation body to pass into the added step
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @param addFirst add the handler before the others
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException;

    /**
     * Get a stream which is attached to the request.
     *
     * @param index the index
     * @return the input stream
     */
    InputStream getAttachmentStream(int index);

    /**
     * Gets the number of streams attached to the request.
     *
     * @return  the number of streams
     */
    int getAttachmentStreamCount();

    /**
     * Get the node into which the operation result should be written.
     *
     * @return the result node
     */
    ModelNode getResult();

    /**
     * Returns whether {@link #getResult()} has been invoked.
     *
     * @return {@code true} if {@link #getResult()} has been invoked
     */
    boolean hasResult();

    /**
     * Complete a step, returning the overall operation result.  The step handler calling this operation should append
     * its result status to the operation result before calling this method.  The return value should be checked
     * to determine whether the operation step should be rolled back.
     * <p>
     * <strong>Note:</strong>This {@code completeStep} variant results in a recursive invocation of the next
     * {@link OperationStepHandler} that has been added (if any). When operations involve a great number of steps that
     * use this variant (e.g. during server or  Host Controller boot), deep call stacks can result and
     * {@link StackOverflowError}s are a possibility. For this reason, handlers that are likely to be executed during
     * boot are encouraged to use the non-recursive {@link #completeStep(RollbackHandler)} variant.
     * </p>
     *
     * @return the operation result action to take
     */
    ResultAction completeStep();

    /**
     * Complete a step, while registering for
     * {@link RollbackHandler#handleRollback(OperationContext, ModelNode)} a notification if the work done by the
     * caller needs to be rolled back}.
     *
     * @param rollbackHandler the handler for any rollback notification. Cannot be {@code null}.
     */
    void completeStep(RollbackHandler rollbackHandler);

    /**
     * Get the failure description result node, creating it if necessary.
     *
     * @return the failure description
     */
    ModelNode getFailureDescription();

    /**
     * Returns whether {@link #getFailureDescription()} has been invoked.
     *
     * @return {@code true} if {@link #getFailureDescription()} has been invoked
     */
    boolean hasFailureDescription();

    /**
     * Get the type of process in which this operation is executing.
     *
     * @return the process type. Will not be {@code null}
     */
    ProcessType getProcessType();

    /**
     * Gets the running mode of the process.
     *
     * @return   the running mode. Will not be {@code null}
     */
    RunningMode getRunningMode();

    /**
     * Get the operation context type.  This can be used to determine whether an operation is executing on a
     * server or on a host controller, etc.
     *
     * @return the operation context type
     *
     * @deprecated Use {@link OperationContext#getProcessType()} and {@link OperationContext#getRunningMode()} or for the most common usage, {@link OperationContext#isNormalServer()}
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    Type getType();

    /**
     * Determine whether the controller is currently performing boot tasks.
     *
     * @return whether the controller is currently booting
     */
    boolean isBooting();

    /**
     * Convenience method to check if the {@link #getProcessType() process type} is {@link ProcessType#isServer() a server type}
     * and the {@link #getRunningMode() running mode} is {@link RunningMode#NORMAL}. The typical usage would
     * be for handlers that are only meant to execute on a normally running server, not on a host controller
     * or on a {@link RunningMode#ADMIN_ONLY} server.
     *
     * @return {@code true} if the {@link #getProcessType() process type} is {@link ProcessType#isServer() a server type}
     *         and the {@link #getRunningMode() running mode} is {@link RunningMode#NORMAL}.
     */
    boolean isNormalServer();

    /**
     * Determine whether the current operation is bound to be rolled back.
     *
     * @return {@code true} if the operation will be rolled back
     */
    boolean isRollbackOnly();

    /**
     * Indicate that the operation should be rolled back, regardless of outcome.
     */
    void setRollbackOnly();

    /**
     * Gets whether any changes made by the operation should be rolled back if an error is introduced
     * by a {@link Stage#RUNTIME} or {@link Stage#VERIFY} handler.
     *
     * @return {@code true} if the operation should rollback if there is a runtime stage failure
     */
    boolean isRollbackOnRuntimeFailure();

    /**
     * Gets whether {@link Stage#RUNTIME} handlers can restart (or remove) runtime services in order to
     * make the operation take effect. If {@code false} and the operation cannot be effected without restarting
     * or removing services, the handler should invoke {@link #reloadRequired()} or {@link #restartRequired()}.
     *
     * @return {@code true} if a service restart or removal is allowed
     */
    boolean isResourceServiceRestartAllowed();

    /**
     * Notify the context that the process requires a stop and re-start of its root service (but not a full process
     * restart) in order to ensure stable operation and/or to bring its running state in line with its persistent configuration.
     *
     * @see ControlledProcessState.State#RELOAD_REQUIRED
     */
    void reloadRequired();

    /**
     * Notify the context that the process must be terminated and replaced with a new process in order to ensure stable
     * operation and/or to bring the running state in line with the persistent configuration.
     *
     * @see ControlledProcessState.State#RESTART_REQUIRED
     */
    void restartRequired();

    /**
     * Notify the context that a previous call to {@link #reloadRequired()} can be ignored (typically because the change
     * that led to the need for reload has been rolled back.)
     */
    void revertReloadRequired();

    /**
     * Notify the context that a previous call to {@link #restartRequired()} can be ignored (typically because the change
     * that led to the need for restart has been rolled back.)
     */
    void revertRestartRequired();

    /**
     * Notify the context that an update to the runtime that would normally have been made could not be made due to
     * the current state of the process. As an example, a step handler that can only update the runtime when
     * {@link #isBooting()} is {@code true} must invoke this method if it is executed when {@link #isBooting()}
     * is {@code false}.
     */
    void runtimeUpdateSkipped();

    /**
     * Get a read only view of the managed resource registration.  The registration is relative to the operation address.
     *
     * @return the model node registration
     */
    ImmutableManagementResourceRegistration getResourceRegistration();

    /**
     * Get a mutable view of the managed resource registration.  The registration is relative to the operation address.
     *
     * @return the model node registration
     */
    ManagementResourceRegistration getResourceRegistrationForUpdate();

    /**
     * Get the service registry.  If the step is not a runtime operation handler step, an exception will be thrown.  The
     * returned registry must not be used to remove services, if an attempt is made to call {@code ServiceController.setMode(REMOVE)}
     * on a {@code ServiceController} returned from this registry an {@code IllegalStateException} will be thrown. To
     * remove a service call {@link removeService(ServiceName name)}.
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
     *
     * @deprecated Use {@link #readResource(PathAddress)}
     */
    @Deprecated
    ModelNode readModel(PathAddress address);

    /**
     * Read a model location, relative to the executed operation address, for the purpose of updating the submodel.
     * This is a write operation, and because only one operation
     * may write at a time, this operation may block until other writing operations have completed.
     *
     * @param address the (possibly empty) address to read
     * @return the model data
     *
     * @deprecated Use {@link #readResourceForUpdate(PathAddress)}
     */
    @Deprecated
    ModelNode readModelForUpdate(PathAddress address);

    /**
     * Acquire the controlling {@link ModelController}'s exclusive lock. Holding this lock prevent other operations
     * from mutating the model, the {@link ManagementResourceRegistration management resource registry} or the runtime
     * service registry until the lock is released. The lock is automatically released when the
     * {@link OperationStepHandler#execute(OperationContext, org.jboss.dmr.ModelNode) execute method} of the handler
     * that invoked this method returns.
     * <p>
     * This method should be little used. The model controller's exclusive lock is acquired automatically when any
     * of the operations in this interface that imply mutating the model, management resource registry or service
     * registry are invoked. The only use for this method are special situations where an exclusive lock is needed
     * but none of those methods will be invoked.
     * </p>
     */
    void acquireControllerLock();

    /**
     * Create a new resource, relative to the executed operation address.  Since only one operation
     * may write at a time, this operation may block until other writing operations have completed.
     *
     * @param address the (possibly empty) address where the resource should be created. Address is relative to the
     *                address of the operation being executed
     * @return the created resource
     * @throws IllegalStateException if a resource already exists at the given address
     * @throws UnsupportedOperationException if the calling operation is not a model operation
     */
    Resource createResource(PathAddress address) throws UnsupportedOperationException;

    /**
     * Add a new resource, relative to the executed operation address.  Since only one operation
     * may write at a time, this operation may block until other writing operations have completed.
     *
     * @param address the (possibly empty) address where the resource should be added. Address is relative to the
     *                address of the operation being executed
     * @param toAdd the new resource
     * @throws IllegalStateException if a resource already exists at the given address
     * @throws UnsupportedOperationException if the calling operation is not a model operation
     */
    void addResource(PathAddress address, Resource toAdd);

    /**
     * Get the addressable resource for read only operations.
     *
     * @param address the address
     * @return the resource
     */
    Resource readResource(PathAddress address);

    /**
     * Get an addressable resource for update operations.
     *
     * @param address the address
     * @return the resource
     */
    Resource readResourceForUpdate(PathAddress address);

    /**
     * Remove a resource relative to the executed operation address. Since only one operation
     * may write at a time, this operation may block until other writing operations have completed.
     *
     * @param address the (possibly empty) address to remove
     * @return the old value of the node
     * @throws UnsupportedOperationException if the calling operation is not a model operation
     */
    Resource removeResource(PathAddress address) throws UnsupportedOperationException;

    /**
     * Get a read-only reference of the entire management model.  The structure of the returned model may depend
     * on the context type (domain vs. server).
     *
     * @return the read-only resource
     */
    Resource getRootResource();

     /**
     * Get a read-only reference of the entire management model BEFORE any changes were made by this context.
     * The structure of the returned model may depend on the context type (domain vs. server).
     *
     * @return the read-only original resource
     */
    Resource getOriginalRootResource();

    /**
     * Determine whether the model has thus far been affected by this operation.
     *
     * @return {@code true} if the model was affected, {@code false} otherwise
     */
    boolean isModelAffected();

    /**
     * Determine whether the {@link ManagementResourceRegistration management resource registry} has thus far been affected by this operation.
     *
     * @return {@code true} if the management resource registry was affected, {@code false} otherwise
     */
    boolean isResourceRegistryAffected();

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
     * Send a message to the client.  Valid only during this operation.
     *
     * @param severity the message severity
     * @param message the message
     */
    void report(MessageSeverity severity, String message);


    /**
     * Marks a resource to indicate that it's backing service(s) will be restarted.
     * This is to ensure that a restart only occurs once, even if there are multiple updates.
     * When true is returned the caller has "aquired" the mark and should proceed with the
     * restart, when false, the caller should take no additional action.
     *
     * The passed owner is compared by instance when a call to {@link #revertReloadRequired()}.
     * This is to ensure that only the "owner" will be successful in reverting the mark.
     *
     * @param resource the resource that will be restarted
     * @param owner the instance representing ownership of the mark
     * @return true if the mark was required and the service should be restarted,
     *         false if no action should be taken.
     */
    boolean markResourceRestarted(PathAddress resource, Object owner);


    /**
     * Removes the restarted marking on the specified resource, provided the passed owner is the one
     * originally used to obtain the mark. The purpose of this method is to facilitate rollback processing.
     * Only the "owner" of the mark should be the one to revert the service to a previous state (once again
     * restarting it).When true is returned, the caller must take the required corrective
     * action by restarting the resource, when false is returned the caller should take no additional action.
     *
     * The passed owner is compared by instance to the one provided in {@link #markResourceRestarted(PathAddress, Object)}
     *
     * @param resource the resource being reverted
     * @param owner the owner of the mark for the resource
     * @return true if the caller owns the mark and the service should be restored by restarting
     *         false if no action should be taken.
     */
    boolean revertResourceRestarted(PathAddress resource, Object owner);

    /**
     * Resolves any expressions in the passed in ModelNode.
     * Expressions may either represent system properties or vaulted date. For vaulted data the format is
     * ${VAULT::vault_block::attribute_name::sharedKey}
     *
     * @param node the ModelNode containing expressions.
     * @return a copy of the node with expressions resolved
     *
     * @throws OperationFailedException if there is a value of type {@link ModelType#EXPRESSION} in the node tree and
     *            there is no system property or environment variable that matches the expression, or if a security
     *            manager exists and its {@link SecurityManager#checkPermission checkPermission} method doesn't allow
     *            access to the relevant system property or environment variable
     */
    ModelNode resolveExpressions(ModelNode node) throws OperationFailedException;

    /**
     * Retrieves an object that has been attached to this context.
     *
     * @param key the key to the attachment.
     * @param <T> the value type of the attachment.
     *
     * @return the attachment if found otherwise {@code null}.
     */
    <T> T getAttachment(AttachmentKey<T> key);

    /**
     * Attaches an arbitrary object to this context.
     *
     * @param key   they attachment key used to ensure uniqueness and used for retrieval of the value.
     * @param value the value to store.
     * @param <T>   the value type of the attachment.
     *
     * @return the previous value associated with the key or {@code null} if there was no previous value.
     */
    <T> T attach(AttachmentKey<T> key, T value);

    /**
     * Attaches an arbitrary object to this context only if the object was not already attached. If a value has already
     * been attached with the key provided, the current value associated with the key is returned.
     *
     * @param key   they attachment key used to ensure uniqueness and used for retrieval of the value.
     * @param value the value to store.
     * @param <T>   the value type of the attachment.
     *
     * @return the previous value associated with the key or {@code null} if there was no previous value.
     */
    <T> T attachIfAbsent(AttachmentKey<T> key, T value);

    /**
     * Detaches or removes the value from this context.
     *
     * @param key the key to the attachment.
     * @param <T> the value type of the attachment.
     *
     * @return the attachment if found otherwise {@code null}.
     */
    <T> T detach(AttachmentKey<T> key);

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
         * The step performs any actions needed to cause the operation to take effect on the relevant servers
         * in the domain. Adding a step in this stage is only allowed when the process type is {@link ProcessType#HOST_CONTROLLER}.
         */
        DOMAIN,
        /**
         * The operation has completed execution.
         */
        DONE;

        Stage() {
        }

        boolean hasNext() {
            return this != DONE;
        }

        Stage next() {
            switch (this) {
                case MODEL: return RUNTIME;
                case RUNTIME: return VERIFY;
                case VERIFY: return DOMAIN;
                case DOMAIN: return DONE;
                case DONE:
                default: throw new IllegalStateException();
            }
        }
    }

    /**
     * The type of controller this operation is being applied to.
     *
     * @deprecated Use {@link OperationContext#getProcessType()} and {@link OperationContext#getRunningMode()}
     */
    @Deprecated
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
         * A server instance which is in {@link RunningMode#ADMIN_ONLY admin-only} mode.
         */
        MANAGEMENT;

        /**
         *  Provides the {@code Type} that matches the given {@code processType} and {@code runningMode}.
         *
         *  @param processType the process type. Cannot be {@code null}
         *  @param runningMode the running mode. Cannot be {@code null}
         *
         *  @return the type
         */
        @SuppressWarnings("deprecation")
        static Type getType(final ProcessType processType, final RunningMode runningMode) {
            if (processType.isServer()) {
                if (runningMode == RunningMode.NORMAL) {
                    return SERVER;
                } else {
                    return MANAGEMENT;
                }
            } else {
                return HOST;
            }
        }
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

    interface RollbackHandler {

        /**
         * A {@link RollbackHandler} that does nothing in the callback. Intended for use by operation step
         * handlers that do not need to do any clean up work -- e.g. those that only perform reads or those
         * that only perform persistent configuration changes. (Persistent configuration changes need not be
         * explicitly rolled back as the {@link OperationContext} will handle that automatically.)
         */
        RollbackHandler NOOP_ROLLBACK_HANDLER = new RollbackHandler() {
            /**
             * Does nothing.
             *
             * @param context  ignored
             * @param operation ignored
             */
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                // no-op
            }
        };

        /**
         * Callback to an {@link OperationStepHandler} indicating that the overall operation is being
         * rolled back and the handler should revert any change it has made. A handler executing in
         * {@link Stage#MODEL} need not revert any changes it has made to the configuration model; this
         * will be done automatically.
         *
         * @param context  the operation execution context; will be the same as what was passed to the
         *                 {@link OperationStepHandler#execute(OperationContext, ModelNode)} method invocation
         *                 that registered this rollback handler.
         * @param operation the operation being rolled back; will be the same as what was passed to the
         *                 {@link OperationStepHandler#execute(OperationContext, ModelNode)} method invocation
         *                 that registered this rollback handler.
         */
        void handleRollback(OperationContext context, ModelNode operation);
    }

    /**
     * An attachment key instance.
     *
     * @param <T> the attachment value type
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final class AttachmentKey<T> {
        private final Class<T> valueClass;

        /**
         * Construct a new instance.
         *
         * @param valueClass the value type.
         */
        private AttachmentKey(final Class<T> valueClass) {
            this.valueClass = valueClass;
        }

        /**
         * Cast the value to the type of this attachment key.
         *
         * @param value the value
         *
         * @return the cast value
         */
        public T cast(final Object value) {
            return valueClass.cast(value);
        }

        /**
         * Construct a new simple attachment key.
         *
         * @param valueClass the value class
         * @param <T>        the attachment type
         *
         * @return the new instance
         */
        @SuppressWarnings("unchecked")
        public static <T> AttachmentKey<T> create(final Class<? super T> valueClass) {
            return new AttachmentKey(valueClass);
        }
    }
}
