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
package org.jboss.as.controller.operations.common;


import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.descriptions.common.ExtensionDescription;
import org.jboss.dmr.ModelNode;

/**
 * Base handler for the extension resource add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
// TODO consider making this concrete and folding in subclass logic
public abstract class  AbstractExtensionAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getAddExtensionOperation(ModelNode address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        return op;
    }

    /**
     * Create the AbstractAddExtensionHandler
     */
    protected AbstractExtensionAddHandler() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        String module = address.getLastElement().getValue();
        model.get(ExtensionDescription.MODULE).set(module);
        installExtension(module, model);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ExtensionDescription.getExtensionAddOperation(locale);
    }

    protected abstract void installExtension(String module, ModelNode model) throws OperationFailedException;

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }
}
