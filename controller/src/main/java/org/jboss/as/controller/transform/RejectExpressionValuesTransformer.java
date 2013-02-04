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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.transform.AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * A transformer rejecting values containing an expression.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class RejectExpressionValuesTransformer implements ResourceTransformer, OperationTransformer {

    private final Set<String> attributeNames;
    private final Map<String, AttributeTransformationRequirementChecker> attributeCheckers;
    private final OperationTransformer writeAttributeTransformer = new WriteAttributeTransformer();


    public RejectExpressionValuesTransformer(AttributeDefinition... attributes) {
        this(namesFromDefinitions(attributes));
    }

    private static Set<String> namesFromDefinitions(AttributeDefinition... attributes) {
        final Set<String> names = new HashSet<String>();
        for (final AttributeDefinition def : attributes) {
            names.add(def.getName());
        }
        return names;
    }

    public RejectExpressionValuesTransformer(Set<String> attributeNames) {
        this(attributeNames, null);
    }

    public RejectExpressionValuesTransformer(String... attributeNames) {
        this(new HashSet<String>(Arrays.asList(attributeNames)));
    }

    public RejectExpressionValuesTransformer(Set<String> allAttributeNames, Map<String, AttributeTransformationRequirementChecker> specialCheckers) {
        this.attributeNames = allAttributeNames;
        this.attributeCheckers = specialCheckers;
    }

    public RejectExpressionValuesTransformer(Map<String, AttributeTransformationRequirementChecker> specialCheckers) {
        this(specialCheckers.keySet(), specialCheckers);
    }

    public RejectExpressionValuesTransformer(String attributeName, AttributeTransformationRequirementChecker checker) {
        this(Collections.singletonMap(attributeName, checker));
    }

    /**
     * Get a "write-attribute" operation transformer.
     *
     * @return a write attribute operation transformer
     */
    public OperationTransformer getWriteAttributeTransformer() {
        return writeAttributeTransformer;
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
        // Check the model
        final Set<String> attributes = checkModel(operation, context);
        final boolean reject = attributes.size() > 0;
        final OperationRejectionPolicy rejectPolicy;
        if (reject) {
            rejectPolicy = new OperationRejectionPolicy() {
                @Override
                public boolean rejectOperation(ModelNode preparedResult) {
                    // Reject successful operations
                    return true;
                }

                @Override
                public String getFailureDescription() {
                    return context.getLogger().getAttributeWarning(address, operation, MESSAGES.attributesDontSupportExpressions(), attributes);
                }
            };
        } else {
            rejectPolicy = OperationTransformer.DEFAULT_REJECTION_POLICY;
        }
        // Return untransformed
        return new TransformedOperation(operation, rejectPolicy, OperationResultTransformer.ORIGINAL_RESULT);
    }

    @Override
    public void transformResource(final ResourceTransformationContext context, final PathAddress address,
                                  final Resource resource) throws OperationFailedException {
        // Check the model
        final ModelNode model = resource.getModel();
        final Set<String> attributes = checkModel(model, context);
        if (attributes.size() > 0) {
            if (context.getTarget().isIgnoredResourceListAvailableAtRegistration()) {
                // Slave is 7.2.x or higher and we know this resource is not ignored
                List<String> msg = Collections.singletonList(context.getLogger().getAttributeWarning(address, null, MESSAGES.attributesDontSupportExpressions(), attributes));

                final TransformationTarget tgt = context.getTarget();
                final String legacyHostName = tgt.getHostName();
                final ModelVersion coreVersion = tgt.getVersion();
                final String subsystemName = findSubsystemName(address);
                final ModelVersion usedVersion = subsystemName == null ? coreVersion : tgt.getSubsystemVersion(subsystemName);

                // Target is  7.2.x or higher so we should throw an error
                if (subsystemName != null) {
                    throw ControllerMessages.MESSAGES.rejectAttributesSubsystemModelResourceTransformer(address, legacyHostName, subsystemName, usedVersion, msg);
                }
                throw ControllerMessages.MESSAGES.rejectAttributesCoreModelResourceTransformer(address, legacyHostName, usedVersion, msg);
            } else {
                // 7.1.x slave; resource *may* be ignored so we can't fail; just log
                context.getLogger().logAttributeWarning(address, MESSAGES.attributesDontSupportExpressions(), attributes);
            }
        }

        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

    private static String findSubsystemName(PathAddress pathAddress) {
        for (PathElement element : pathAddress) {
            if (element.getKey().equals(SUBSYSTEM)) {
                return element.getValue();
            }
        }
        return null;
    }

    /**
     * Check the model for expression values.
     *
     * @param model the model
     * @return the attribute containing an expression
     */
    private Set<String> checkModel(final ModelNode model, TransformationContext context) throws OperationFailedException {
        final Set<String> attributes = new HashSet<String>();
        AttributeTransformationRequirementChecker checker;
        for (final String attribute : attributeNames) {
            if (model.hasDefined(attribute)) {
                if (attributeCheckers != null && (checker = attributeCheckers.get(attribute)) != null) {
                    if (checker.isAttributeTransformationRequired(attribute, model.get(attribute), context)) {
                        attributes.add(attribute);
                    }
                } else if (SIMPLE_EXPRESSIONS.isAttributeTransformationRequired(attribute, model.get(attribute), context)) {
                    attributes.add(attribute);
                }
            }
        }
        return attributes;
    }

    class WriteAttributeTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            boolean containsExpression = false;
            if (attributeNames.contains(attribute)) {
                if (operation.hasDefined(VALUE)) {
                    AttributeTransformationRequirementChecker checker;
                    if (attributeCheckers != null && (checker = attributeCheckers.get(attribute)) != null) {
                        if (checker.isAttributeTransformationRequired(attribute, operation.get(VALUE), context)) {
                            containsExpression = true;
                        }
                    } else if (SIMPLE_EXPRESSIONS.isAttributeTransformationRequired(attribute, operation.get(VALUE), context)) {
                        containsExpression = true;
                    }
                }
            }
            final boolean rejectResult = containsExpression;
            if (rejectResult) {
                // Create the rejection policy
                final OperationRejectionPolicy rejectPolicy = new OperationRejectionPolicy() {
                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        // Reject successful operations
                        return true;
                    }

                    @Override
                    public String getFailureDescription() {
                        return context.getLogger().getAttributeWarning(address, operation, MESSAGES.attributesDontSupportExpressions(), attribute);
                    }
                };
                return new TransformedOperation(operation, rejectPolicy, OperationResultTransformer.ORIGINAL_RESULT);
            }
            // In case it's not an expressions just forward unmodified
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }


}
