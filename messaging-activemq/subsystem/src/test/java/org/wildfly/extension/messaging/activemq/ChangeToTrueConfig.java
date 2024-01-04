/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
class ChangeToTrueConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<ChangeToTrueConfig> {

    private final String attribute;

    ChangeToTrueConfig(String attribute) {
        super(attribute);
        this.attribute = attribute;
    }

    @Override
    protected boolean isAttributeWritable(String attributeName) {
        return true;
    }

    @Override
    protected boolean checkValue(ModelNode operation, String attrName, ModelNode attribute, boolean isGeneratedWriteAttribute) {
        if (!isGeneratedWriteAttribute && operation.get(ModelDescriptionConstants.OP).asString().equals(WRITE_ATTRIBUTE_OPERATION) && operation.hasDefined(NAME) && operation.get(NAME).asString().equals(this.attribute)) {
            // The attribute won't be defined in the :write-attribute(name=<attribute name>,.. boot operation so don't reject in that case
            return false;
        }
        return !attribute.equals(ModelNode.TRUE);
    }

    @Override
    protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
        throw new IllegalStateException();
    }

    @Override
    protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
        return ModelNode.TRUE;
    }
}
