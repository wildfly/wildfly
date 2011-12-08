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
import java.util.List;

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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * {@link OperationContext} implementation for parallel handling of subsystem operations during boot.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ParallelBootOperationContext extends AbstractOperationContext {

    private final OperationContext primaryContext;
    private final List<ParsedBootOp> runtimeOps;

    ParallelBootOperationContext(final ModelController.OperationTransactionControl transactionControl,
                                 final ControlledProcessState processState, final OperationContext primaryContext,
                                 final List<ParsedBootOp> runtimeOps,
                                 final Thread controllingThread) {
        super(Type.SERVER, transactionControl, processState, true);
        this.primaryContext = primaryContext;
        this.runtimeOps = runtimeOps;
        AbstractOperationContext.controllingThread.set(controllingThread);
    }

    @Override
    public void addStep(OperationStepHandler step, Stage stage) throws IllegalArgumentException {
        if (activeStep == null) {
            throw MESSAGES.noActiveStep();
        }
        addStep(activeStep.response, activeStep.operation, step, stage);
    }

    @Override
    public void addStep(ModelNode operation, OperationStepHandler step, Stage stage) throws IllegalArgumentException {
        if (activeStep == null) {
            throw MESSAGES.noActiveStep();
        }
        addStep(activeStep.response, operation, step, stage);
    }

    @Override
    public void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage) throws IllegalArgumentException {
        switch (stage) {
            case MODEL:
            case IMMEDIATE:
                super.addStep(response, operation, step, stage);
                break;
            case RUNTIME:
                if (runtimeOps != null) {
                    // Cache for use by the runtime step from ParallelBootOperationStepHandler
                    ParsedBootOp parsedOp = new ParsedBootOp(operation, step, response);
                    runtimeOps.add(parsedOp);
                } else {
                    super.addStep(response, operation, step, stage);
                }
                break;
            default:
                // Handle VERIFY in the primary context, after parallel work is done
                primaryContext.addStep(response, operation, step, stage);
        }
    }

    // Methods unimplemented by superclass

    @Override
    public InputStream getAttachmentStream(int index) {
        return primaryContext.getAttachmentStream(index);
    }

    @Override
    public int getAttachmentStreamCount() {
        return primaryContext.getAttachmentStreamCount();
    }

    @Override
    public boolean isRollbackOnRuntimeFailure() {
        return primaryContext.isRollbackOnRuntimeFailure();
    }

    @Override
    public boolean isResourceServiceRestartAllowed() {
        return primaryContext.isResourceServiceRestartAllowed();
    }

    @Override
    public ImmutableManagementResourceRegistration getResourceRegistration() {
        ImmutableManagementResourceRegistration parent = primaryContext.getResourceRegistration();
        return  parent.getSubModel(activeStep.address);
    }

    @Override
    public ManagementResourceRegistration getResourceRegistrationForUpdate() {
        ManagementResourceRegistration parent = primaryContext.getResourceRegistrationForUpdate();
        return  parent.getSubModel(activeStep.address);
    }

    @Override
    public ServiceRegistry getServiceRegistry(boolean modify) throws UnsupportedOperationException {
        return primaryContext.getServiceRegistry(modify);
    }

    @Override
    public ServiceController<?> removeService(ServiceName name) throws UnsupportedOperationException {
        return primaryContext.removeService(name);
    }

    @Override
    public void removeService(ServiceController<?> controller) throws UnsupportedOperationException {
        primaryContext.removeService(controller);
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
        PathAddress fullAddress = activeStep.address.append(address);
        return primaryContext.readModelForUpdate(fullAddress);
    }

    @Override
    public void acquireControllerLock() {
        // ignore; ParallelBootOperationStepHandler should already have the controller lock
    }

    @Override
    public Resource createResource(PathAddress address) throws UnsupportedOperationException {
        PathAddress fullAddress = activeStep.address.append(address);
        return primaryContext.createResource(fullAddress);
    }

    @Override
    public void addResource(PathAddress address, Resource toAdd) {
        PathAddress fullAddress = activeStep.address.append(address);
        primaryContext.addResource(fullAddress, toAdd);
    }

    @Override
    public Resource readResource(PathAddress address) {
        PathAddress fullAddress = activeStep.address.append(address);
        return primaryContext.readResource(fullAddress);
    }

    @Override
    public Resource readResourceForUpdate(PathAddress address) {
        PathAddress fullAddress = activeStep.address.append(address);
        return primaryContext.readResourceForUpdate(fullAddress);
    }

    @Override
    public Resource removeResource(PathAddress address) throws UnsupportedOperationException {
        PathAddress fullAddress = activeStep.address.append(address);
        return primaryContext.removeResource(fullAddress);
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
        return primaryContext.getCurrentStage();
    }

    @Override
    public void report(MessageSeverity severity, String message) {
        primaryContext.report(severity, message);
    }

    @Override
    public boolean markResourceRestarted(PathAddress resource, Object owner) {
        throw new UnsupportedOperationException("Resource restarting is not supported during boot");
    }

    @Override
    public boolean revertResourceRestarted(PathAddress resource, Object owner) {
        throw new UnsupportedOperationException("Resource restarting is not supported during boot");
    }

    @Override
    void awaitModelControllerContainerMonitor() throws InterruptedException {
        // ignored
    }

    @Override
    ConfigurationPersister.PersistenceResource createPersistenceResource() throws ConfigurationPersistenceException {
        // We don't persist
        return null;
    }

    @Override
    void releaseStepLocks(Step step) {
        // Our steps took no locks
    }

    @Override
    public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
        return primaryContext.resolveExpressions(node);
    }

}
