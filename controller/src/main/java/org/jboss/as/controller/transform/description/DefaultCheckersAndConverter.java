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

import java.util.Collections;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;


/**
 * Utility class combining the functionality of {@link DiscardAttributeChecker.DefaultDiscardAttributeChecker}, {@link RejectAttributeChecker.DefaultRejectAttributeChecker}
 * and {@link AttributeConverter.DefaultAttributeConverter}. Only the parts that are registered in {@link AttributeTransformationDescriptionBuilder} will be used.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class DefaultCheckersAndConverter extends DiscardAttributeChecker.DefaultDiscardAttributeChecker implements RejectAttributeChecker, AttributeConverter {

    private volatile String logMessageId;

    /**
     * Constructor
     *
     * @param discardExpressions {@code true} if the attribute should be discarded if expressions are used
     * @param discardUndefined {@code true} if the attribute should be discarded if expressions are used
     */
    protected DefaultCheckersAndConverter(final boolean discardExpressions, final boolean discardUndefined) {
        super(discardExpressions, discardUndefined);
    }

    /**
     * Constructor.
     *
     * Sets it up with {@code discardExpressions==false} and {@code discardUndefined==true}
     *
     */
    public DefaultCheckersAndConverter() {
        this(false, true);
    }


    /** {@inheritDoc} */
    @Override
    public void convertOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
        convertAttribute(address, attributeName, attributeValue, context);
    }

    /** {@inheritDoc} */
    @Override
    public void convertResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        convertAttribute(address, attributeName, attributeValue, context);
    }

    /** {@inheritDoc} */
    public  boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
        return rejectAttribute(address, attributeName, attributeValue, context);
    }

    /** {@inheritDoc} */
    public boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        return rejectAttribute(address, attributeName, attributeValue, context);
    }

    /**
     * Returns the log message id used by this checker. This is used to group it so that all attributes failing a type of rejction
     * end up in the same error message. This default implementation uses the formatted log message with an empty attribute map as the id.
     *
     * @return the log message id
     */
    public String getRejectionLogMessageId() {
        String id = logMessageId;
        if (id == null) {
            id = getRejectionLogMessage(Collections.<String, ModelNode>emptyMap());
        }
        logMessageId = id;
        return logMessageId;
    }
    /**
     * Gets called by the default implementations of {@link #rejectOperationParameter(String, ModelNode, ModelNode, TransformationContext)} and
     * {@link #rejectResourceAttribute(String, ModelNode, TransformationContext)}.
     *
     * @param address the address of the operation
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param context the context of the transformation
     *
     * @return {@code true} if the attribute or parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
     */
    protected abstract boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context);

    /**
     * Gets called by the default implementations of {@link #convertOperationParameter(PathAddress, String, ModelNode, ModelNode, TransformationContext)} and
     * {@link #convertResourceAttribute(PathAddress, String, ModelNode, TransformationContext)}.
     *
     * @param address the address of the operation or resource
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param context the context of the transformation
     *
     * @return {@code true} if the attribute or parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
     */
    protected abstract void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context);

}
