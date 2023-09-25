/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.dmr.ModelNode;

/**
 * Simple {@link AttributeConverter} that provides the operation or model context to a {@link Converter}.
 * @author Paul Ferraro
 */
public class SimpleAttributeConverter implements AttributeConverter {

    public interface Converter {
        void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context);
    }

    private final Converter converter;

    public SimpleAttributeConverter(Converter converter) {
        this.converter = converter;
    }

    @Override
    public final void convertOperationParameter(PathAddress address, String name, ModelNode value, ModelNode operation, TransformationContext context) {
        this.converter.convert(address, name, value, operation, context);
    }

    @Override
    public final void convertResourceAttribute(PathAddress address, String name, ModelNode value, TransformationContext context) {
        this.converter.convert(address, name, value, context.readResource(PathAddress.EMPTY_ADDRESS).getModel(), context);
    }
}
