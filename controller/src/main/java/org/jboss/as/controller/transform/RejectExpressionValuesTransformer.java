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

package org.jboss.as.controller.transform;

import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * A transformer rejecting values containing an expression.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class RejectExpressionValuesTransformer implements ResourceTransformer, OperationTransformer {

    private final ChainedResourceTransformer resourceDelegate;
    @SuppressWarnings("deprecation")
    private final RejectExpressionValuesChainedTransformer chainedExpressionTransformer;

    public RejectExpressionValuesTransformer(AttributeDefinition... attributes) {
        chainedExpressionTransformer = new RejectExpressionValuesChainedTransformer(attributes);
        resourceDelegate = new ChainedResourceTransformer(chainedExpressionTransformer);
    }

    public RejectExpressionValuesTransformer(Set<String> attributeNames) {
        chainedExpressionTransformer = new RejectExpressionValuesChainedTransformer(attributeNames);
        resourceDelegate = new ChainedResourceTransformer(chainedExpressionTransformer);
    }

    public RejectExpressionValuesTransformer(String... attributeNames) {
        chainedExpressionTransformer = new RejectExpressionValuesChainedTransformer(attributeNames);
        resourceDelegate = new ChainedResourceTransformer(chainedExpressionTransformer);
    }

    /**
     * This should be called for resource transformations done for 7.1.x, as we have no idea if the slave has ignored the resource or not. If
     * this method is called, we log an error if expressions were used rather than throw an error (since the slave might have ignored the resource).
     * On 7.2.x the slave registers the ingored resources as part of the registration process so we have a better idea and can throw errors if the
     * resource is not ignored.
     *
     * @return this transformer
     */
    public RejectExpressionValuesTransformer setWarnOnResource() {
        chainedExpressionTransformer.setWarnOnResource();
        return this;
    }

    /**
     * Get a "write-attribute" operation transformer.
     *
     * @return a write attribute operation transformer
     */
    public OperationTransformer getWriteAttributeTransformer() {
        return chainedExpressionTransformer.getWriteAttributeTransformer();
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
        return chainedExpressionTransformer.transformOperation(context, address, operation);
    }

    @Override
    public void transformResource(final ResourceTransformationContext context, final PathAddress address,
                                  final Resource resource) throws OperationFailedException {
        resourceDelegate.transformResource(context, address, resource);
    }
}
