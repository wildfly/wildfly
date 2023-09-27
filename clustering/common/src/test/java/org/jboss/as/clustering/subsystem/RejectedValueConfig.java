/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.subsystem;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.dmr.ModelNode;

/**
 * Generic {@link AttributesPathAddressConfig} for a rejected attribute.
 * @author Paul Ferraro
 */
public class RejectedValueConfig extends AttributesPathAddressConfig<RejectedValueConfig> {

    private final Predicate<ModelNode> rejection;
    private final UnaryOperator<ModelNode> corrector;

    public RejectedValueConfig(Attribute attribute, Predicate<ModelNode> rejection) {
        this(attribute, rejection, value -> attribute.getDefinition().getDefaultValue());
    }

    public RejectedValueConfig(Attribute attribute, Predicate<ModelNode> rejection, UnaryOperator<ModelNode> corrector) {
        super(attribute.getDefinition().getName());
        this.rejection = rejection;
        this.corrector = corrector;
    }

    @Override
    protected boolean isAttributeWritable(String attributeName) {
        return true;
    }

    @Override
    protected boolean checkValue(String attrName, ModelNode attribute, boolean isGeneratedWriteAttribute) {
        return this.rejection.test(attribute);
    }

    @Override
    protected ModelNode correctValue(ModelNode toResolve, boolean isGeneratedWriteAttribute) {
        return this.corrector.apply(toResolve);
    }
}
