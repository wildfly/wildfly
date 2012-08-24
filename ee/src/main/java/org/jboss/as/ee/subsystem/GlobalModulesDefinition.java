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

package org.jboss.as.ee.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.ee.EeMessages.MESSAGES;

import java.util.Locale;
import java.util.ResourceBundle;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersOfValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ListAttributeDefinition} implementation for the "global-modules" attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class GlobalModulesDefinition extends ListAttributeDefinition {

    public static final String NAME = "name";
    public static final String SLOT = "slot";
    public static final String GLOBAL_MODULES = "global-modules";

    private static final ParameterValidator moduleValidator;

    public static final String DEFAULT_SLOT = "main";

    static {
        final ParametersValidator delegate = new ParametersValidator();
        delegate.registerValidator(NAME, new StringLengthValidator(1));
        delegate.registerValidator(SLOT, new StringLengthValidator(1, true));

        moduleValidator = new ParametersOfValidator(delegate);
    }

    public static final GlobalModulesDefinition INSTANCE = new GlobalModulesDefinition();

    private GlobalModulesDefinition() {
        super(GLOBAL_MODULES, true, moduleValidator);
    }

    @Override
    public ModelNode resolveModelAttribute(OperationContext context, ModelNode operationObject) throws OperationFailedException {
        final ModelNode result = super.validateOperation(operationObject);

        if (result.isDefined()) {
            for (ModelNode module : result.asList()) {
                if (!module.hasDefined(SLOT)) {
                    module.get(SLOT).set(DEFAULT_SLOT);
                }
            }
        }

        return result;
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        // This method being used indicates a misuse of this class
        throw MESSAGES.resourceDescriptionResolverError();
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(NAME, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, NAME));
        valueType.get(SLOT, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, SLOT));
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(NAME, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, NAME));
        valueType.get(SLOT, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, SLOT));
    }

    @Override
    public void marshallAsElement(ModelNode eeSubSystem, boolean marshalDefault, XMLStreamWriter writer) throws XMLStreamException {
        if (eeSubSystem.hasDefined(getName()) && eeSubSystem.asInt() > 0) {
            writer.writeStartElement(Element.GLOBAL_MODULES.getLocalName());
            final ModelNode globalModules = eeSubSystem.get(getName());
            for (ModelNode module : globalModules.asList()) {
                writer.writeEmptyElement(Element.MODULE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), module.get(NAME).asString());
                if (module.hasDefined(SLOT)) {
                    writer.writeAttribute(Attribute.SLOT.getLocalName(), module.get(SLOT).asString());
                }
            }
            writer.writeEndElement();
        }
    }

    private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
        final ModelNode valueType = parent.get(VALUE_TYPE);
        final ModelNode name = valueType.get(NAME);
        name.get(DESCRIPTION); // placeholder
        name.get(TYPE).set(ModelType.STRING);
        name.get(NILLABLE).set(false);
        final ModelNode slot = valueType.get(SLOT);
        slot.get(DESCRIPTION);  // placeholder
        slot.get(TYPE).set(ModelType.STRING);
        slot.get(NILLABLE).set(true);
        slot.get(DEFAULT).set(DEFAULT_SLOT);

        return valueType;
    }
}
