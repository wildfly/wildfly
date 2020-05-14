/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.clustering.controller.Operations;
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
        if (!isGeneratedWriteAttribute && Operations.getName(operation).equals(WRITE_ATTRIBUTE_OPERATION) && operation.hasDefined(NAME) && operation.get(NAME).asString().equals(this.attribute)) {
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
