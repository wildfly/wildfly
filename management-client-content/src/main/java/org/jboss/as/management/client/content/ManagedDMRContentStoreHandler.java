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

package org.jboss.as.management.client.content;

import java.util.Arrays;
import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Add handler for a resource that represents a named bit of re-usable DMR.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagedDMRContentStoreHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "store";

    private final AttributeDefinition contentAttribute;
    private final ResourceDescriptionResolver descriptionResolver;

    public ManagedDMRContentStoreHandler(final AttributeDefinition contentAttribute, ResourceDescriptionResolver descriptionResolver) {
        this.contentAttribute = contentAttribute;
        this.descriptionResolver = descriptionResolver;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final byte[] oldHash = ManagedDMRContentResourceDefinition.HASH.validateOperation(operation).asBytes();
        final byte[] currentHash = resource.getModel().get(ManagedDMRContentResourceDefinition.HASH.getName()).asBytes();
        if (!Arrays.equals(oldHash, currentHash)) {
            throw ManagedDMRContentMessages.MESSAGES.invalidHash(
                    HashUtil.bytesToHexString(oldHash),
                    PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)),
                    HashUtil.bytesToHexString(currentHash));
        }
        ModelNode model = new ModelNode();
        contentAttribute.validateAndSet(operation, model);

        // IMPORTANT: Use writeModel, as this is what causes the content to be flushed to the content repo!
        resource.writeModel(model);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        DescriptionProvider delegate = new DefaultOperationDescriptionProvider(OPERATION_NAME, descriptionResolver, ManagedDMRContentResourceDefinition.HASH, contentAttribute);
        return delegate.getModelDescription(locale);
    }
}
