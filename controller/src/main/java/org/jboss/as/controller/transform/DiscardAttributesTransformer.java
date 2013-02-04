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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.transform.OperationResultTransformer.ORIGINAL_RESULT;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Discards attributes silently. This class should ONLY be used if you are 100% sure a new attribute can be discarded, even if set.
 * Provide a {@link DiscardApprover} to the constructor to provide specific logic for making the determination as to
 * whether the transformation should be done. It is made abstract to make you think about using it.
 * <p>
 * Normally, you would want to use {@link DiscardUndefinedAttributesTransformer} instead.
 * </p>
 * <p>
 * A typical use case for this transformer would be in combination with {@link DiscardUndefinedAttributesTransformer}.
 * First this transformer would run, with a {@link DiscardApprover} checking the state of the model or operation to
 * decide whether removing attributes is valid. The discard approver would only approve the removal if the value of
 * the model or operation parameters is such that the servers launched by a slave Host Controller running the legacy
 * version and unaware of the removed attributes would function consistently with newer version servers who saw the
 * attributes. This transformer would remove the attributes in that case, and leave them otherwise. Then the
 * {@link DiscardUndefinedAttributesTransformer} would run and would log a warning or fail operations if any of
 * the attributes were left. So this transformer cleans if possible, and {@link DiscardUndefinedAttributesTransformer}
 * deals with any problems left after cleaning.
 * </p>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class DiscardAttributesTransformer implements OperationTransformer, ResourceTransformer {

    /**
     * Approves the transformation of the resource or operation.
     */
    public interface DiscardApprover {

        /**
         * Replies with whether resource transformation should be done.
         *
         * @param context the context
         * @param address the address of the resource to transform
         * @param resource the resource
         * @return {@code  true} if transformation should be done; {@code false} if the resource should be left as is
         */
        boolean isResourceDiscardAllowed(TransformationContext context, PathAddress address, Resource resource);
        /**
         * Replies with whether operation transformation should be done.
         *
         * @param context the context
         * @param address the address of the resource to transform
         * @param operation the operation
         * @return {@code  true} if transformation should be done; {@code false} if the operation should be left as is
         */
        boolean isOperationDiscardAllowed(TransformationContext context, PathAddress address, ModelNode operation);
    }

    /**
     * A {@code DiscardApprover} that always returns {@code true}; <strong>use with extreme caution.</strong> It would
     * be a very unusual situation for it to be appropriate to always transform a resource or operation regardless
     * of the values in the model or operation.
     */
    public static final DiscardApprover LENIENT_DISCARD_APPROVER = new DiscardApprover() {
        @Override
        public boolean isResourceDiscardAllowed(TransformationContext context, PathAddress address, Resource resource) {
            return true;
        }

        @Override
        public boolean isOperationDiscardAllowed(TransformationContext context, PathAddress address, ModelNode operation) {
            return true;
        }
    };

    /**
     * A {@code DiscardApprover} that checks the value of a provided attribute in order to decide whether transformation
     * is allowed. If the attribute has the desired value (or is undefined if allowing that is configured) then the
     * transformation is allowed.
     * <p>
     * <strong>This approver can only be used for {@code add}, {@code write-attribute} and {@code undefine-attribute}
     * operation transformation</strong> as well as resource transformation.
     * </p>
     */
    public static class AttributeValueDiscardApprover implements DiscardApprover {

        private final String attributeName;
        private final ModelNode approvedValue;
        private final boolean allowUndefined;

        /**
         * Creates a new transformer.
         *
         * @param attributeName the name of the attribute to check
         * @param approvedValue the value the attribute must have in order to allow transformation
         * @param allowUndefined {@code true} if the attribute can also be undefined
         */
        public AttributeValueDiscardApprover(String attributeName, ModelNode approvedValue, boolean allowUndefined) {
            this.attributeName = attributeName;
            this.approvedValue = approvedValue;
            this.allowUndefined = allowUndefined;
        }

        @Override
        public boolean isResourceDiscardAllowed(TransformationContext context, PathAddress address, Resource resource) {
            return isDiscardAllowed(resource.getModel());
        }

        /**
         * Checks if the value of the resource after the operation is applied would meet the criteria. Can only be used
         * for {@code add}, {@code write-attribute} and {@code undefine-attribute} operation transformation.
         *
         * {@inheritDoc}
         *
         * @throws IllegalStateException if the operation name isn't {@code add}, {@code write-attribute} or {@code undefine-attribute}
         */
        @Override
        public boolean isOperationDiscardAllowed(TransformationContext context, PathAddress address, ModelNode operation) {
            String opName = operation.require(OP).asString();
            if (ADD.equals(opName)) {
                return isDiscardAllowed(operation);
            } else if (WRITE_ATTRIBUTE_OPERATION.equals(opName)) {
                String attr = operation.require(NAME).asString();
                if (attr.equals(attributeName)) {
                    // A desire to change the relevant attribute
                    ModelNode value = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();
                    ModelNode mockModel = new ModelNode();
                    mockModel.get(attributeName).set(value);
                    return isDiscardAllowed(mockModel);
                } else {
                    // A desire to change a different attribute.
                    // See if the target attribute has a legal value to permit the change
                    return isDiscardAllowed(context.readResource(PathAddress.EMPTY_ADDRESS).getModel());
                }
            } else if (UNDEFINE_ATTRIBUTE_OPERATION.equals(opName)) {
                String attr = operation.require(NAME).asString();
                if (attr.equals(attributeName)) {
                    // A desire to change the relevant attribute
                    ModelNode mockModel = new ModelNode();
                    mockModel.get(attributeName); // leave undefined
                    return isDiscardAllowed(mockModel);
                } else {
                    // A desire to change a different attribute.
                    // See if the target attribute has a legal value to permit the change
                    return isDiscardAllowed(context.readResource(PathAddress.EMPTY_ADDRESS).getModel());
                }
            }
            // We aren't valid for use with this operation
            throw new IllegalStateException();
        }

        private boolean isDiscardAllowed(final ModelNode modelNode) {
            if (modelNode.hasDefined(attributeName)) {
                return approvedValue.equals(modelNode.get(attributeName));
            }
            return allowUndefined;
        }
    }

    private final DiscardApprover discardApprover;
    private final Set<String> attributeNames;
    private final OperationTransformer writeAttributeTransformer = new WriteAttributeTransformer();
    private final OperationTransformer undefineAttributeTransformer = writeAttributeTransformer;

    /**
     * Creates a new transformer.
     *
     * @param attributes  the attributes to discard
     *
     * @deprecated use a variant that takes a {@link DiscardApprover}
     */
    @Deprecated
    protected DiscardAttributesTransformer(AttributeDefinition... attributes) {
        this(LENIENT_DISCARD_APPROVER, namesFromDefinitions(attributes));
    }

    /**
     * Creates a new transformer.
     * @param discardApprover approves whether or not transformation should be done. Cannot be {@code null}
     * @param attributes the attributes to discard
     */
    protected DiscardAttributesTransformer(DiscardApprover discardApprover, AttributeDefinition... attributes) {
        this(discardApprover, namesFromDefinitions(attributes));
    }

    private static Set<String> namesFromDefinitions(AttributeDefinition... attributes) {
        final Set<String> names = new HashSet<String>();
        for(final AttributeDefinition def : attributes) {
            names.add(def.getName());
        }
        return names;
    }

    /**
     * Creates a new transformer.
     *
     * @param attributeNames  the attributes to discard
     *
     * @deprecated use a variant that takes a {@link DiscardApprover}
     */
    @Deprecated
    protected DiscardAttributesTransformer(String... attributeNames) {
        this(LENIENT_DISCARD_APPROVER, new HashSet<String>(Arrays.asList(attributeNames)));
    }

    /**
     * Creates a new transformer.
     * @param discardApprover approves whether or not transformation should be done. Cannot be {@code null}
     * @param attributeNames  the attributes to discard
     */
    protected DiscardAttributesTransformer(DiscardApprover discardApprover, String... attributeNames) {
        this(discardApprover, new HashSet<String>(Arrays.asList(attributeNames)));
    }

    /**
     * Creates a new transformer.
     *
     * @param attributeNames  the attributes to discard
     *
     * @deprecated use a variant that takes a {@link DiscardApprover}
     */
    @Deprecated
    public DiscardAttributesTransformer(Set<String> attributeNames) {
        this(LENIENT_DISCARD_APPROVER, attributeNames);
    }

    /**
     * Creates a new transformer.
     * @param discardApprover approves whether or not transformation should be done. Cannot be {@code null}
     * @param attributeNames  the attributes to discard
     */
    public DiscardAttributesTransformer(DiscardApprover discardApprover, Set<String> attributeNames) {
        assert discardApprover != null : "discardApprover is null";
        assert attributeNames != null : "attributeNames is null";

        this.attributeNames = attributeNames;
        this.discardApprover = discardApprover;
    }

    public OperationTransformer getWriteAttributeTransformer() {
        return writeAttributeTransformer;
    }

    public OperationTransformer getUndefineAttributeTransformer() {
        return undefineAttributeTransformer;
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
            throws OperationFailedException {
        boolean allowDiscard = discardApprover.isOperationDiscardAllowed(context, address, operation);
        final ModelNode transformedOperation = allowDiscard ? transformInternal(operation.clone()) : operation;
        return new TransformedOperation(transformedOperation, ORIGINAL_RESULT);
    }

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource)
            throws OperationFailedException {
        if (discardApprover.isResourceDiscardAllowed(context, address, resource)) {
            transformInternal(resource.getModel());
        }
    }

    private ModelNode transformInternal(ModelNode model) {
        for (String attr : attributeNames) {
            model.remove(attr);
        }
        return model;
    }

    private class WriteAttributeTransformer implements OperationTransformer {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address,
                ModelNode operation) throws OperationFailedException {
            if (attributeNames.contains(operation.get(NAME).asString())
                    && discardApprover.isOperationDiscardAllowed(context, address, operation)) {
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }
            return OperationTransformer.DEFAULT.transformOperation(context, address, operation);
        }
    }
}
