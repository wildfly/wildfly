/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.transformers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesChainedTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * The older versions of the model do not allow {@code null} for the system property boottime attribute.
 * If it is {@code null}, make sure it is {@code true}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SystemPropertyTransformers {

    @SuppressWarnings("deprecation")
    static final RejectExpressionValuesChainedTransformer rejectExpressions = new RejectExpressionValuesChainedTransformer(SystemPropertyResourceDefinition.VALUE).setWarnOnResource();


    static TransformersSubRegistration registerTransformers(TransformersSubRegistration parent) {
        TransformersSubRegistration reg = parent.registerSubResource(SystemPropertyResourceDefinition.PATH,
                new ChainedResourceTransformer(
                        new ChainedResourceTransformerEntry() {

                            @Override
                            public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource)
                                    throws OperationFailedException {
                                ModelNode model = resource.getModel();
                                if (!model.hasDefined(BOOT_TIME)) {
                                    model.get(BOOT_TIME).set(true);
                                }
                            }
                        },
                        rejectExpressions));

        reg.registerOperationTransformer(ADD,
                new ChainedOperationTransformer(
                    new OperationTransformer() {
                        @Override
                        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                                throws OperationFailedException {
                            ModelNode transformed = operation.clone();
                            if (!transformed.hasDefined(BOOT_TIME)) {
                                transformed.get(BOOT_TIME).set(true);
                            }
                            return new TransformedOperation(transformed, OperationResultTransformer.ORIGINAL_RESULT);
                        }
                    },
                    rejectExpressions));

        OperationTransformer forceTrue = new OperationTransformer() {

            @Override
            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                    throws OperationFailedException {
                ModelNode transformed = operation;
                if (transformed.get(NAME).asString().equals(BOOT_TIME)) {
                    transformed = transformed.clone();
                    if (!transformed.get(VALUE).isDefined()) {
                        transformed.get(VALUE).set(true);
                        if (UNDEFINE_ATTRIBUTE_OPERATION.equals(transformed.get(OP).asString())){
                            transformed.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                        }
                    }
                }
                return new TransformedOperation(transformed, OperationResultTransformer.ORIGINAL_RESULT);
            }
        };
        reg.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(forceTrue, rejectExpressions));
        reg.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(forceTrue, rejectExpressions));

        return reg;
    }


}
