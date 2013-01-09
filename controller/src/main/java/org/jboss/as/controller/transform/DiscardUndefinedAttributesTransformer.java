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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
import org.jboss.dmr.ModelNode;

/**
 * Transformer that hides new attributes from legacy slaves if the attribute value is undefined. A defined value
 * leads to a log warning or an {@link OperationFailedException} unless the resource is ignored by the target.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class DiscardUndefinedAttributesTransformer implements ChainedResourceTransformerEntry, ResourceTransformer, OperationTransformer {

    private final Set<String> attributeNames;
    private final OperationTransformer writeAttributeTransformer = new WriteAttributeTransformer();
    private final OperationTransformer undefineAttributeTransformer = new UndefineAttributeTransformer();

    public DiscardUndefinedAttributesTransformer(AttributeDefinition... attributes) {
        this(namesFromDefinitions(attributes));
    }

    private static Set<String> namesFromDefinitions(AttributeDefinition... attributes) {
        final Set<String> names = new HashSet<String>();
        for(final AttributeDefinition def : attributes) {
            names.add(def.getName());
        }
        return names;
    }

    public DiscardUndefinedAttributesTransformer(String... attributeNames) {
        this(new HashSet<String>(Arrays.asList(attributeNames)));
    }

    public DiscardUndefinedAttributesTransformer(Set<String> attributeNames) {
        this.attributeNames = attributeNames;
    }

    public OperationTransformer getWriteAttributeTransformer() {
        return writeAttributeTransformer;
    }

    public OperationTransformer getUndefineAttributeTransformer() {
        return undefineAttributeTransformer;
    }

    @Override
    public void transformResource(ChainedResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
        transformResource(context.getTarget(), address, resource);
    }

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
        transformResource(context.getTarget(), address, resource);

        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address,
                                                   final ModelNode operation) throws OperationFailedException {

        final Set<String> problems = checkModelNode(operation);
        final boolean reject = problems != null;
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
                    // TODO OFE.getMessage
                    try {
                        return logWarning(context.getTarget(), address, problems, operation);
                    } catch (OperationFailedException e) {
                        //This will not happen
                        return null;
                    }
                }
            };
        } else {
            rejectPolicy = OperationTransformer.DEFAULT_REJECTION_POLICY;
        }
        // Return untransformed
        return new TransformedOperation(operation, rejectPolicy, OperationResultTransformer.ORIGINAL_RESULT);
    }

    private void transformResource(TransformationTarget target, PathAddress address,
                                   Resource resource) throws OperationFailedException {

        Set<String> problems = checkModelNode(resource.getModel());
        if (problems != null) {
            logWarning(target, address, problems, null);
        }
    }

    private Set<String> checkModelNode(ModelNode modelNode) {

        Set<String> problems = null;
        for (String attr : attributeNames) {
            if (modelNode.has(attr)) {
                if (modelNode.hasDefined(attr)) {
                    if (problems == null) {
                        problems = new HashSet<String>();
                    }
                    problems.add(attr);
                } else {
                    modelNode.remove(attr);
                }
            }
        }
        return problems;
    }

    private class WriteAttributeTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            boolean ourAttribute = attributeNames.contains(attribute);
            final boolean rejectResult = ourAttribute && operation.hasDefined(VALUE);
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
                        try {
                            return logWarning(context.getTarget(), address, Collections.singleton(attribute), operation);
                        } catch (OperationFailedException e) {
                            //This will not happen
                            return null;
                        }
                    }
                };
                return new TransformedOperation(operation, rejectPolicy, OperationResultTransformer.ORIGINAL_RESULT);
            } else if (ourAttribute) {
                // It's an attribute the slave doesn't understand, but the new value is "undefined"
                // Just discard this operation
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }
            // Not relevant to us
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }

    private class UndefineAttributeTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            if (attributeNames.contains(attribute)) {
                // It's an attribute the slave doesn't understand, but the new value is "undefined"
                // Just discard this operation
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }
            // Not relevant to us
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }

    private String logWarning(TransformationTarget tgt, PathAddress pathAddress, Set<String> attributes, ModelNode op) throws OperationFailedException {

        //TODO the determining of whether the version is 1.4.0, i.e. knows about ignored resources or not could be moved to a utility method

        final String hostName = tgt.getHostName();
        final ModelVersion coreVersion = tgt.getVersion();
        final String subsystemName = findSubsystemVersion(pathAddress);
        final ModelVersion usedVersion = subsystemName == null ? coreVersion : tgt.getSubsystemVersion(subsystemName);

        //For 7.1.x, we have no idea if the slave has ignored the resource or not. On 7.2.x the slave registers the ignored resources as
        //part of the registration process so we have a better idea and can throw errors if the slave was ignored
        if (op == null) {
            if (coreVersion.getMajor() >= 1 && coreVersion.getMinor() >= 4) {
                //We are 7.2.x so we should throw an error
                if (subsystemName != null) {
                    throw ControllerMessages.MESSAGES.newAttributesSubsystemModelResourceTransformerFoundDefinedAttributes(pathAddress, hostName, subsystemName, usedVersion, attributes);
                }
                throw ControllerMessages.MESSAGES.newAttributesCoreModelResourceTransformerFoundDefinedAttributes(pathAddress, hostName, usedVersion, attributes);
            }
        }

        if (op == null) {
            if (subsystemName != null) {
                ControllerLogger.TRANSFORMER_LOGGER.newAttributesSubsystemModelResourceTransformerFoundDefinedAttributes(pathAddress, hostName, subsystemName, usedVersion, attributes);
            } else {
                ControllerLogger.TRANSFORMER_LOGGER.newAttributesCoreModelResourceTransformerFoundDefinedAttributes(pathAddress, hostName, usedVersion, attributes);
            }
            return null;
        } else {
            if (subsystemName != null) {
                return ControllerMessages.MESSAGES.newAttributesSubsystemModelOperationTransformerFoundDefinedAttributes(op, pathAddress, hostName, subsystemName, usedVersion, attributes).getMessage();
            } else {
                return ControllerMessages.MESSAGES.newAttributesCoreModelOperationTransformerFoundDefinedAttributes(op, pathAddress, hostName, usedVersion, attributes).getMessage();
            }
        }
    }


    private String findSubsystemVersion(PathAddress pathAddress) {
        for (PathElement element : pathAddress) {
            if (element.getKey().equals(SUBSYSTEM)) {
                return element.getValue();
            }
        }
        return null;
    }
}
