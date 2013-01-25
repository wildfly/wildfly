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

package org.jboss.as.controller.transform.description;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class OperationTransformationOverrideBuilderImpl extends AttributeTransformationDescriptionBuilderImpl<OperationTransformationOverrideBuilder> implements OperationTransformationOverrideBuilder {

    private boolean inherit = false;
    private DiscardPolicy discardPolicy = DiscardPolicy.NEVER;
    private OperationTransformer transformer = OperationTransformer.DEFAULT;
    private String newName;

    private final String operationName;

    protected OperationTransformationOverrideBuilderImpl(final String operationName, final ResourceTransformationDescriptionBuilder builder) {
        super(builder, new AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry());
        this.operationName = operationName;
    }

    @Override
    public OperationTransformationOverrideBuilder inheritResourceAttributeDefinitions() {
        this.inherit = true;
        return this;
    }

    public OperationTransformationOverrideBuilder setCustomOperationTransformer(OperationTransformer transformer) {
        this.transformer = transformer;
        return this;
    }

    @Override
    public OperationTransformationOverrideBuilder rename(String newName) {
        this.newName = newName;
        return this;
    }

    protected OperationTransformer createTransformer(final AttributeTransformationDescriptionBuilderRegistry resourceRegistry) {
        final AttributeTransformationDescriptionBuilderRegistry registry = resultingRegistry(resourceRegistry);
        final Map<String, AttributeTransformationDescription> descriptions = registry.buildAttributes();
        if(WRITE_ATTRIBUTE_OPERATION.equals(operationName)) {
            // Custom write-attribute operation
            final TransformationRule first = new OperationTransformationRules.WriteAttributeRule(descriptions);
            return createTransformer(first);
        } else if(UNDEFINE_ATTRIBUTE_OPERATION.equals(operationName)) {
            // Custom undefine-attribute operation
            final TransformationRule first = new OperationTransformationRules.UndefineAttributeRule(descriptions);
            return createTransformer(first);
        } else {
            final AttributeTransformationRule first = new AttributeTransformationRule(descriptions);
            return createTransformer(first);
        }
    }

    protected OperationTransformer createTransformer(final TransformationRule first) {
        return new OperationTransformer() {
            @Override
            public TransformedOperation transformOperation(final TransformationContext ctx, final PathAddress address, final ModelNode operation) throws OperationFailedException {

                final DiscardPolicy.DiscardType type = discardPolicy.discard(operation, address, ctx);
                if(type == DiscardPolicy.DiscardType.SILENT) {
                    return OperationTransformer.DISCARD.transformOperation(ctx, address, operation);
                } // TODO handle other cases

                final Iterator<TransformationRule> iterator = Collections.<TransformationRule>emptyList().iterator();
                final ModelNode originalModel = TransformationRule.cloneAndProtect(operation);
                final TransformationRule.ChainedOperationContext context = new TransformationRule.ChainedOperationContext(ctx) {

                    @Override
                    void invokeNext(OperationTransformer.TransformedOperation transformedOperation) throws OperationFailedException {
                        recordTransformedOperation(transformedOperation);
                        if(iterator.hasNext()) {
                            final TransformationRule next = iterator.next();
                            // TODO hmm, do we need to change the address?
                            next.transformOperation(transformedOperation.getTransformedOperation(), address, this);
                        } else {
                            if (newName != null) {
                                transformedOperation.getTransformedOperation().get(OP).set(newName);
                            }
                            final TransformationContext ctx = getContext();
                            recordTransformedOperation(transformer.transformOperation(ctx, address, transformedOperation.getTransformedOperation()));
                        }
                    }
                };
                operation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());
                // Kick off the chain
                first.transformOperation(operation, address, context);
                // Create the composite operation result
                return context.createOp();
            }
        };
    }

    protected AttributeTransformationDescriptionBuilderRegistry resultingRegistry(final AttributeTransformationDescriptionBuilderRegistry resourceRegistry) {
        final AttributeTransformationDescriptionBuilderRegistry local = getLocalRegistry();
        if(inherit) {
            return AttributeTransformationDescriptionBuilderImpl.mergeRegistries(resourceRegistry, local);
        }
        return local;
    }

    @Override
    protected OperationTransformationOverrideBuilder thisBuilder() {
        return this;
    }

}
