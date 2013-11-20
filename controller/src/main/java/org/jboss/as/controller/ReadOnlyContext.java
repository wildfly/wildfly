/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.io.InputStream;
import java.util.Set;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.ResourceAuthorization;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * A read-only {@linkplain OperationContext}, allowing read-only access to the current write model from a different
 * operation, preventing any writes from this context. Operations can acquire a controller lock to prevent other
 * writes happen until this operation is done.
 *
 * @author Emanuel Muckenhuber
 */
class ReadOnlyContext extends AbstractOperationContext {

    private final int operationId;
    private final ModelControllerImpl controller;
    private final AbstractOperationContext primaryContext;
    private Step lockStep;

    ReadOnlyContext(final ProcessType processType, final RunningMode runningMode, final ModelController.OperationTransactionControl transactionControl,
                    final ControlledProcessState processState, final boolean booting,
                    final AbstractOperationContext primaryContext, final ModelControllerImpl controller, final int operationId) {
        super(processType, runningMode, transactionControl, processState, booting, controller.getAuditLogger());
        this.primaryContext = primaryContext;
        this.controller = controller;
        this.operationId = operationId;
    }

    @Override
    void awaitModelControllerContainerMonitor() throws InterruptedException {
        // nothing here
    }

    @Override
    ConfigurationPersister.PersistenceResource createPersistenceResource() throws ConfigurationPersistenceException {
        throw readOnlyContext();
    }

    @Override
    void waitForRemovals() throws InterruptedException {
        // nothing here
    }

    @Override
    public InputStream getAttachmentStream(int index) {
        throw readOnlyContext();
    }

    @Override
    public int getAttachmentStreamCount() {
        return 0;
    }

    @Override
    public boolean isRollbackOnRuntimeFailure() {
        return false;
    }

    @Override
    public boolean isResourceServiceRestartAllowed() {
        return false;
    }

    @Override
    public ImmutableManagementResourceRegistration getResourceRegistration() {
        return primaryContext.getResourceRegistration();
    }

    @Override
    public ManagementResourceRegistration getResourceRegistrationForUpdate() {
        throw readOnlyContext();
    }

    @Override
    public ImmutableManagementResourceRegistration getRootResourceRegistration() {
        return primaryContext.getRootResourceRegistration();
    }

    @Override
    public ServiceRegistry getServiceRegistry(boolean modify) throws UnsupportedOperationException {
        if (modify) {
            throw readOnlyContext();
        }
        return primaryContext.getServiceRegistry(false);
    }

    @Override
    public ServiceController<?> removeService(ServiceName name) throws UnsupportedOperationException {
        throw readOnlyContext();
    }

    @Override
    public void removeService(ServiceController<?> controller) throws UnsupportedOperationException {
        throw readOnlyContext();
    }

    @Override
    public ServiceTarget getServiceTarget() throws UnsupportedOperationException {
        return primaryContext.getServiceTarget();
    }

    @Override
    public ModelNode readModel(PathAddress address) {
        PathAddress fullAddress = activeStep.address.append(address);
        return primaryContext.readModel(fullAddress);
    }

    @Override
    public ModelNode readModelForUpdate(PathAddress address) {
        throw readOnlyContext();
    }

    @Override
    public void acquireControllerLock() {
        if (lockStep == null) {
            try {
                controller.acquireLock(operationId, true, this);
                lockStep = activeStep;
            } catch (InterruptedException e) {
                cancelled = true;
                Thread.currentThread().interrupt();
                throw MESSAGES.operationCancelledAsynchronously();
            }
        }
    }

    @Override
    void releaseStepLocks(Step step) {
        if (step == lockStep) {
            lockStep = null;
            controller.releaseLock(operationId);
        }
    }

    @Override
    public Resource createResource(PathAddress address) throws UnsupportedOperationException {
        throw readOnlyContext();
    }

    @Override
    public void addResource(PathAddress address, Resource toAdd) {
        throw readOnlyContext();
    }

    @Override
    public Resource readResource(PathAddress address) {
        PathAddress fullAddress = activeStep.address.append(address);
        return primaryContext.readResource(fullAddress);
    }

    @Override
    public Resource readResource(PathAddress address, boolean recursive) {
        PathAddress fullAddress = activeStep.address.append(address);
        return primaryContext.readResource(fullAddress, recursive);
    }

    @Override
    public Resource readResourceFromRoot(PathAddress address) {
        return primaryContext.readResourceFromRoot(address);
    }

    @Override
    public Resource readResourceFromRoot(PathAddress address, boolean recursive) {
        return primaryContext.readResourceFromRoot(address, recursive);
    }

    @Override
    public Resource readResourceForUpdate(PathAddress relativeAddress) {
        throw readOnlyContext();
    }

    @Override
    public Resource removeResource(PathAddress relativeAddress) throws UnsupportedOperationException {
        throw readOnlyContext();
    }

    @Override
    public Resource getRootResource() {
        return primaryContext.getRootResource();
    }

    @Override
    public Resource getOriginalRootResource() {
        return primaryContext.getOriginalRootResource();
    }

    @Override
    public boolean isModelAffected() {
        return primaryContext.isModelAffected();
    }

    @Override
    public boolean isResourceRegistryAffected() {
        return primaryContext.isResourceRegistryAffected();
    }

    @Override
    public boolean isRuntimeAffected() {
        return primaryContext.isRuntimeAffected();
    }

    @Override
    public Stage getCurrentStage() {
        return currentStage;
    }

    @Override
    public void report(MessageSeverity severity, String message) {
        // primaryContext.report(severity, message);
    }

    @Override
    public boolean markResourceRestarted(PathAddress resource, Object owner) {
        return false;
    }

    @Override
    public boolean revertResourceRestarted(PathAddress resource, Object owner) {
        return false;
    }

    @Override
    public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
        return primaryContext.resolveExpressions(node);
    }

    @Override
    public <T> T getAttachment(AttachmentKey<T> key) {
        return primaryContext.getAttachment(key);
    }

    @Override
    public <T> T attach(AttachmentKey<T> key, T value) {
        throw readOnlyContext();
    }

    @Override
    public <T> T attachIfAbsent(AttachmentKey<T> key, T value) {
        throw readOnlyContext();
    }

    @Override
    public <T> T detach(AttachmentKey<T> key) {
        throw readOnlyContext();
    }

    @Override
    public AuthorizationResult authorize(ModelNode operation) {
        return primaryContext.authorize(operation);
    }

    @Override
    public AuthorizationResult authorize(ModelNode operation, Set<Action.ActionEffect> effects) {
        return primaryContext.authorize(operation, effects);
    }

    @Override
    public AuthorizationResult authorize(ModelNode operation, String attribute, ModelNode currentValue) {
        return primaryContext.authorize(operation, attribute, currentValue);
    }

    @Override
    public AuthorizationResult authorize(ModelNode operation, String attribute, ModelNode currentValue, Set<Action.ActionEffect> effects) {
        return primaryContext.authorize(operation, attribute, currentValue, effects);
    }

    IllegalStateException readOnlyContext() {
        return ControllerMessages.MESSAGES.readOnlyContext();
    }

    @Override
    public AuthorizationResult authorizeOperation(ModelNode operation) {
        return primaryContext.authorizeOperation(operation);
    }

    @Override
    public ResourceAuthorization authorizeResource(boolean attributes, boolean isDefaultResource) {
        return primaryContext.authorizeResource(attributes, isDefaultResource);
    }

    Resource getModel() {
        return primaryContext.getModel();
    }

}
