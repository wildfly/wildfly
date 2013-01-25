/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;
import static org.jboss.as.domain.controller.transformers.DomainTransformers.IGNORED_SUBSYSTEMS;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat, inc
 */
class JSFSubsystemTransformers {

    private static final String JSF_SUBSYSTEM = "jsf";
    private static final String SLOT_ATTRIBUTE_NAME = "default-jsf-impl-slot";
    private static final String SLOT_DEFAULT_VALUE = "main";

    static void registerTransformers120(TransformerRegistry registry, TransformersSubRegistration parent) {
        registry.registerSubsystemTransformers(JSF_SUBSYSTEM, IGNORED_SUBSYSTEMS, new ResourceTransformer() {
            @Override
            public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {

                ModelNode model = resource.getModel();
                if (model.hasDefined(SLOT_ATTRIBUTE_NAME)) {
                    ModelNode slot = model.get(SLOT_ATTRIBUTE_NAME);
                    if (!SLOT_DEFAULT_VALUE.equals(slot.asString())) {
                        context.getLogger().logWarning(address, SLOT_ATTRIBUTE_NAME, MESSAGES.invalidJSFSlotValue(slot.asString()));
                    }
                }
                Set<String> attributes = new HashSet<String>();
                for (Property prop : resource.getModel().asPropertyList()) {
                    attributes.add(prop.getName());
                }
                attributes.remove(SLOT_ATTRIBUTE_NAME);
                if (!attributes.isEmpty()) {
                    context.getLogger().logWarning(address, ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndWillBeIgnored(), attributes);
                }
            }
        });

        TransformersSubRegistration jsfSubsystem = parent.registerSubResource(PathElement.pathElement(SUBSYSTEM, JSF_SUBSYSTEM));
        jsfSubsystem.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformer() {
            @Override
            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                final String name = operation.require(NAME).asString();
                final ModelNode value = operation.get(ModelDescriptionConstants.VALUE);
                if (!SLOT_ATTRIBUTE_NAME.equals(name)) {
                    context.getLogger().logWarning(address, ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndWillBeIgnored(), name);
                    return DISCARD.transformOperation(context, address, operation);
                }
                if (value.isDefined() && value.equals(SLOT_DEFAULT_VALUE)) {
                    return DISCARD.transformOperation(context, address, operation);
                } else {
                    OperationRejectionPolicy rejectionPolicy = new OperationRejectionPolicy() {
                        @Override
                        public boolean rejectOperation(ModelNode preparedResult) {
                            return true;
                        }

                        @Override
                        public String getFailureDescription() {
                            return MESSAGES.invalidJSFSlotValue(value.asString());
                        }
                    };
                    return new TransformedOperation(operation, rejectionPolicy, OperationResultTransformer.ORIGINAL_RESULT);
                }
            }
        });
        jsfSubsystem.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new OperationTransformer() {
            @Override
            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                String attributeName = operation.require(NAME).asString();
                if (!SLOT_ATTRIBUTE_NAME.equals(attributeName)) {
                    return DEFAULT.transformOperation(context, address, operation);
                } else {
                    context.getLogger().logWarning(address, ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndWillBeIgnored(), attributeName);
                    return DISCARD.transformOperation(context, address, operation);
                }
            }
        });
    }



}
