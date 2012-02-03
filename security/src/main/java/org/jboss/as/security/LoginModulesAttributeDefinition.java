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
package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersOfValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
* @author Jason T. Greene
*/
public class LoginModulesAttributeDefinition extends ListAttributeDefinition {


    public static final ParameterValidator validator;
    public static final ParameterValidator fieldValidator;

    static {
        final ParametersValidator delegate = new ParametersValidator();
        delegate.registerValidator(CODE, new StringLengthValidator(1));
        delegate.registerValidator(Constants.FLAG, new EnumValidator<ModuleFlag>(ModuleFlag.class, false, false));
        delegate.registerValidator(Constants.MODULE, new StringLengthValidator(1,true));
        delegate.registerValidator(Constants.MODULE_OPTIONS, new ModelTypeValidator(ModelType.OBJECT, true));

        validator = new ParametersOfValidator(delegate);
        fieldValidator = delegate;
    }


    public LoginModulesAttributeDefinition(String name, String xmlName) {
        super(name, xmlName, false, 1, Integer.MAX_VALUE, validator, null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        // This method being used indicates a misuse of this class
        throw SecurityMessages.MESSAGES.unsupportedOperationExceptionUseResourceDesc();
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(CODE, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, CODE));
        valueType.get(Constants.FLAG, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.FLAG));
        valueType.get(Constants.MODULE, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.MODULE));
        valueType.get(Constants.MODULE_OPTIONS, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.MODULE_OPTIONS));
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
         final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(CODE, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, CODE));
        valueType.get(Constants.FLAG, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.FLAG));
        valueType.get(Constants.MODULE, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.MODULE));
        valueType.get(Constants.MODULE_OPTIONS, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.MODULE_OPTIONS));
    }

    public static ModelNode parseField(String name, String value, XMLStreamReader reader) throws XMLStreamException {
        final String trimmed = value == null ? null : value.trim();
        ModelNode node;
        if (trimmed != null ) {
            node = new ModelNode().set(trimmed);
        } else {
            node = new ModelNode();
        }

        try {
            fieldValidator.validateParameter(name, node);
        } catch (OperationFailedException e) {
            throw SecurityMessages.MESSAGES.xmlStreamException(e.getFailureDescription().toString(), reader.getLocation());
        }
        return node;
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
      if (resourceModel.hasDefined(getName()) && resourceModel.asInt() > 0) {
            final ModelNode modules = resourceModel.get(getName());
            for (ModelNode module : modules.asList()) {
                writer.writeStartElement(getXmlName());
                writer.writeAttribute(Attribute.CODE.getLocalName(), module.get(CODE).asString());
                writer.writeAttribute(Attribute.FLAG.getLocalName(), module.get(Constants.FLAG).asString().toLowerCase());

                if(module.hasDefined(Constants.MODULE)){
                    writer.writeAttribute(Attribute.MODULE.getLocalName(), module.get(Constants.MODULE).asString());
                }
                if (module.hasDefined(Constants.MODULE_OPTIONS)) {
                    for (ModelNode option : module.get(Constants.MODULE_OPTIONS).asList()) {
                        writer.writeEmptyElement(Element.MODULE_OPTION.getLocalName());
                        writer.writeAttribute(Attribute.NAME.getLocalName(), option.asProperty().getName());
                        writer.writeAttribute(Attribute.VALUE.getLocalName(), option.asProperty().getValue().asString());
                    }
                }
                writer.writeEndElement();
            }
        }
    }

    private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
        final ModelNode valueType = parent.get(VALUE_TYPE);
        final ModelNode code = valueType.get(CODE);
        code.get(DESCRIPTION); // placeholder
        code.get(TYPE).set(ModelType.STRING);
        code.get(NILLABLE).set(false);
        code.get(MIN_LENGTH).set(1);

        final ModelNode flag = valueType.get(Constants.FLAG);
        flag.get(DESCRIPTION);  // placeholder
        flag.get(TYPE).set(ModelType.STRING);
        flag.get(NILLABLE).set(false);

        for (ModuleFlag value : ModuleFlag.values())
            flag.get(ALLOWED).add(value.toString());

        final ModelNode moduleOptions = valueType.get(Constants.MODULE_OPTIONS);
        moduleOptions.get(DESCRIPTION);  // placeholder
        moduleOptions.get(TYPE).set(ModelType.OBJECT);
        moduleOptions.get(VALUE_TYPE).set(ModelType.STRING);
        moduleOptions.get(NILLABLE).set(true);


        return valueType;
    }
}
