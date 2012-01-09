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
package org.jboss.as.controller.resource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESTINATION_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESTINATION_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOURCE_NETWORK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.MaskedAddressValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersOfValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * An attribute definition for client network to destination mappings on a socket binding.
 *
 * @author Jason T. Greene
*/
public class ClientMappingsAttributeDefinition extends ListAttributeDefinition {


    public static final ParameterValidator validator;
    public static final ParameterValidator fieldValidator;

    static {
        final ParametersValidator delegate = new ParametersValidator();
        delegate.registerValidator(SOURCE_NETWORK, new MaskedAddressValidator(true, false));
        delegate.registerValidator(DESTINATION_ADDRESS, new StringLengthValidator(1));
        delegate.registerValidator(DESTINATION_PORT, new IntRangeValidator(0, 65535, true, false));

        validator = new ParametersOfValidator(delegate);
        fieldValidator = delegate;
    }


    public ClientMappingsAttributeDefinition(String name) {
        super(name, name, true, 0, Integer.MAX_VALUE, validator, null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        // This method being used indicates a misuse of this class
        throw new UnsupportedOperationException("Use the ResourceDescriptionResolver variant");
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(SOURCE_NETWORK, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, SOURCE_NETWORK));
        valueType.get(DESTINATION_ADDRESS, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, DESTINATION_ADDRESS));
        valueType.get(DESTINATION_PORT, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, DESTINATION_PORT));
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
         final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(SOURCE_NETWORK, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, SOURCE_NETWORK));
        valueType.get(DESTINATION_ADDRESS, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, DESTINATION_ADDRESS));
        valueType.get(DESTINATION_PORT, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, DESTINATION_PORT));
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
        throw new UnsupportedOperationException("Not implemented");
    }

    private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
        final ModelNode valueType = parent.get(VALUE_TYPE);
        final ModelNode sourceNetwork = valueType.get(SOURCE_NETWORK);
        sourceNetwork.get(DESCRIPTION); // placeholder
        sourceNetwork.get(TYPE).set(ModelType.STRING);
        sourceNetwork.get(NILLABLE).set(true);
        sourceNetwork.get(MIN_LENGTH).set(1);

        final ModelNode destination = valueType.get(DESTINATION_ADDRESS);
        destination.get(DESCRIPTION); // placeholder
        destination.get(TYPE).set(ModelType.STRING);
        destination.get(NILLABLE).set(false);
        destination.get(MIN_LENGTH).set(1);

        final ModelNode port = valueType.get(DESTINATION_PORT);
        port.get(DESCRIPTION); // placeholder
        port.get(TYPE).set(ModelType.INT);
        port.get(NILLABLE).set(true);
        port.get(MIN).set(0);
        port.get(MAX).set(65535);

        return valueType;
    }
}
