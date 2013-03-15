/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * A handler for the {@code add} operation that only manipulates the model.  The original expected use is for
 * resources that have been dropped from recent versions, but for which configuration manageablity is retained in
 * order to allow use on legacy hosts in a managed domain. This handler would be used on the host controllers for
 * the newer version nodes (particularly the master host controller.)
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ModelOnlyAddStepHandler extends AbstractAddStepHandler {

    private final AttributeDefinition[] attributes;

    /**
     * Creates a new {@code ModelOnlyStepHandler} that stores the given attributes to the model.
     *
     * @param attributes the attributes
     */
    public ModelOnlyAddStepHandler(AttributeDefinition... attributes) {
        this.attributes = attributes;
    }

    /**
     * Call {@link AttributeDefinition#validateAndSet(org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)} for each
     * attribute provided to the constructor
     *
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition ad : attributes) {
            ad.validateAndSet(operation, model);
        }
    }

    /**
     * Returns {@code false}.
     *
     * {@inheritDoc}
     */
    @Override
    protected final boolean requiresRuntime(OperationContext context) {
        return false;
    }

    /**
     * Returns {@code false}.
     *
     * {@inheritDoc}
     */
    @Override
    protected final boolean requiresRuntimeVerification() {
        return false;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * {@inheritDoc}
     */
    @Override
    protected final void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * {@inheritDoc}
     */
    @Override
    protected final void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        throw new UnsupportedOperationException();
    }
}
