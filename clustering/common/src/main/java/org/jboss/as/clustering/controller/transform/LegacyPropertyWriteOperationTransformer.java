/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Radoslav Husar
 * @version October 2015
 */
public class LegacyPropertyWriteOperationTransformer implements OperationTransformer {

    @Override
    public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
        if (operation.get(NAME).asString().equals(PROPERTIES)) {

            InitialAttributeValueOperationContextAttachment attachment = context.getAttachment(InitialAttributeValueOperationContextAttachment.INITIAL_VALUES_ATTACHMENT);
            assert attachment != null;

            // Workaround aliases limitations!
            // we need to painstakingly undo path alias translations, since we need to know the address of the real resource,
            // since the readResourceFromRoot() will not work on the aliased address
            Map<String, String> undoAliases = new HashMap<>();
            undoAliases.put("BINARY_KEYED_JDBC_STORE", "binary-jdbc");
            undoAliases.put("STORE", "custom");
            undoAliases.put("FILE_STORE", "file");
            undoAliases.put("MIXED_KEYED_JDBC_STORE", "mixed-jdbc");
            undoAliases.put("REMOTE_STORE", "remote");
            undoAliases.put("STRING_KEYED_JDBC_STORE", "string-jdbc");
            if (undoAliases.containsKey(address.getLastElement().getValue())) {
                address = address.subAddress(0, address.size() - 1).append("store", undoAliases.get(address.getLastElement().getValue()));
            }

            ModelNode initialValue = attachment.getInitialValue(address, Operations.getAttributeName(operation));
            ModelNode newValue = context.readResourceFromRoot(address).getModel().get(PROPERTIES).clone();

            if (initialValue.equals(newValue) || (initialValue.isDefined() && initialValue.asPropertyList().isEmpty() && !newValue.isDefined())) {
                // There is nothing to do, discard this operation
                return new TransformedOperation(null, DEFAULT_REJECTION_POLICY, SUCCESSFUL_RESULT);
            }

            final Map<String, ModelNode> oldMap = new HashMap<>();
            if (initialValue.isDefined()) {
                for (Property property : initialValue.asPropertyList()) {
                    oldMap.put(property.getName(), property.getValue());
                }
            }

            // Transformed address for all operations
            final PathAddress legacyAddress = Operations.getPathAddress(operation);

            // This may result as multiple operations on the legacy node
            final ModelNode composite = new ModelNode();
            composite.get(OP).set(COMPOSITE);
            composite.get(OP_ADDR).setEmptyList();

            if (newValue.isDefined()) {
                for (Property property : newValue.asPropertyList()) {
                    String key = property.getName();
                    ModelNode value = property.getValue();

                    if (!oldMap.containsKey(key)) {
                        // This is a newly added property => :add operation
                        ModelNode addOp = Util.createAddOperation(legacyAddress.append(PathElement.pathElement(PROPERTY, key)));
                        addOp.get(VALUE).set(value);
                        composite.get(STEPS).add(addOp);
                    } else {
                        final ModelNode oldPropValue = oldMap.get(key);
                        if (!oldPropValue.equals(value)) {
                            // Property value is different => :write-attribute operation
                            ModelNode writeOp = Util.getWriteAttributeOperation(legacyAddress.append(PathElement.pathElement(PROPERTY, key)),
                                    VALUE, value);
                            composite.get(STEPS).add(writeOp);
                        }
                        // Otherwise both property name and value are the same => no operation

                        // Remove this key
                        oldMap.remove(key);
                    }
                }
            }

            // Properties that were removed = :remove operation
            for (Map.Entry<String, ModelNode> prop : oldMap.entrySet()) {
                ModelNode removeOperation = Util.createRemoveOperation(legacyAddress.append(PathElement.pathElement(PROPERTY, prop.getKey())));
                composite.get(STEPS).add(removeOperation);
            }

            initialValue.set(newValue.clone());

            return new TransformedOperation(composite, OperationResultTransformer.ORIGINAL_RESULT);
        }
        return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
    }
}
