/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropagatingCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link AttributeDefinition} a thread pool resource's keepalive-time attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class KeepAliveTimeAttributeDefinition extends SimpleAttributeDefinition {

    KeepAliveTimeAttributeDefinition() {
        super(CommonAttributes.KEEPALIVE_TIME, ModelType.OBJECT, true,
            PropagatingCorrector.INSTANCE, new ParameterValidator(){
                @Override
                public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                    if(value.getType() == ModelType.UNDEFINED) {
                        return;
                    }
                    if(value.getType() != ModelType.OBJECT) {
                        throw new OperationFailedException("Attribute " + parameterName +
                                " expects values of type OBJECT but got " + value + " of type " + value.getType());
                    }
                    final Set<String> keys = value.keys();
                    if(keys.size() != 2) {
                        throw new OperationFailedException("Attribute " + parameterName +
                                " expects values consisting of '" + TIME +
                                "' and '" + UNIT + "' but the new value consists of " + keys);
                    }
                    if (!keys.contains(TIME)) {
                        throw new OperationFailedException("Missing '" + TIME + "' for '" + parameterName + "'");
                    }
                    if (!keys.contains(UNIT)) {
                        throw new OperationFailedException("Missing '" + UNIT + "' for '" + parameterName + "'");
                    }
                }
                @Override
                public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
                    validateParameter(parameterName, value);
                }}
        );
    }

    @Override
    public ModelNode addResourceAttributeDescription(ModelNode resourceDescription, ResourceDescriptionResolver resolver,
                                                     Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addResourceAttributeDescription(resourceDescription, resolver, locale, bundle);
        addAttributeValueTypeDescription(result, resolver, locale, bundle);
        return result;
    }

    @Override
    public ModelNode addOperationParameterDescription(ModelNode resourceDescription, String operationName,
                                                      ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addOperationParameterDescription(resourceDescription, operationName, resolver, locale, bundle);
        addOperationParameterValueTypeDescription(result, operationName, resolver, locale, bundle);
        return result;
    }

    private void addAttributeValueTypeDescription(ModelNode result, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        addNoTextValueTypeDescription(result);
        result.get(VALUE_TYPE, TIME, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(KEEPALIVE_TIME, locale, bundle, TIME));
        result.get(VALUE_TYPE, UNIT, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(KEEPALIVE_TIME, locale, bundle, UNIT));
    }

    private void addOperationParameterValueTypeDescription(ModelNode result, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        addNoTextValueTypeDescription(result);
        result.get(VALUE_TYPE, TIME, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, KEEPALIVE_TIME, locale, bundle, TIME));
        result.get(VALUE_TYPE, UNIT, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, KEEPALIVE_TIME, locale, bundle, UNIT));
    }

    private void addNoTextValueTypeDescription(final ModelNode node) {
        node.get(VALUE_TYPE, TIME, TYPE).set(ModelType.LONG);
        node.get(VALUE_TYPE, TIME, REQUIRED).set(true);
        node.get(VALUE_TYPE, UNIT, TYPE).set(ModelType.STRING);
        node.get(VALUE_TYPE, UNIT, REQUIRED).set(true);
    }
}
