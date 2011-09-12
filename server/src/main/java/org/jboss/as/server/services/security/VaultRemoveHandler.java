/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.services.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.VaultDescriptions;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the vault remove operation.
 *
 * @author Anil Saldhana
 * @author Brian Stansberry
 */
public class VaultRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static final VaultRemoveHandler INSTANCE = new VaultRemoveHandler();

    /**
     * Create the VaultRemoveHandler
     */
    protected VaultRemoveHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return VaultDescriptions.getVaultRemoveDescription(locale);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.getType() == OperationContext.Type.HOST || context.getType() == OperationContext.Type.SERVER;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // Let's not try to remove a core service without a reload
        context.reloadRequired();
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.revertReloadRequired();
    }
}