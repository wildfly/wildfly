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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesChainedTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
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
    static final RejectExpressionValuesChainedTransformer rejectExpressions =
            new RejectExpressionValuesChainedTransformer(SystemPropertyResourceDefinition.VALUE, SystemPropertyResourceDefinition.BOOT_TIME);


    static void registerTransformers120(TransformersSubRegistration parent) {

        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(SystemPropertyResourceDefinition.PATH)
            .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, SystemPropertyResourceDefinition.VALUE, SystemPropertyResourceDefinition.BOOT_TIME)
                .setValueConverter(new AttributeConverter.DefaultAttributeConverter(){
                    @Override
                    protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                            TransformationContext context) {
                        if (!attributeValue.isDefined()) {
                            attributeValue.set(true);
                        }
                    }

                }, BOOT_TIME)
                .end()
            .addRawOperationTransformationOverride(UNDEFINE_ATTRIBUTE_OPERATION, new OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                        throws OperationFailedException {
                    if (operation.get(NAME).asString().equals(BOOT_TIME)) {
                        ModelNode op = operation.clone();
                        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                        op.get(VALUE).set(true);
                        return new TransformedOperation(op, OperationResultTransformer.ORIGINAL_RESULT);
                    }
                    return OperationTransformer.DEFAULT.transformOperation(context, address, operation);
                }
            });

        TransformationDescription.Tools.register(builder.build(), parent);
    }


}
