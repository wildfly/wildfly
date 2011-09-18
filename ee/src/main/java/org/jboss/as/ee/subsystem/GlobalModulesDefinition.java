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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
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

    private static final ParameterValidator moduleValidator;

    static {
        final ParametersValidator delegate = new ParametersValidator();
        delegate.registerValidator(CommonAttributes.NAME, new StringLengthValidator(1));
        delegate.registerValidator(CommonAttributes.SLOT, new StringLengthValidator(1, true));

        moduleValidator = new ParametersOfValidator(delegate);
    }

    public static final GlobalModulesDefinition INSTANCE = new GlobalModulesDefinition();

    private GlobalModulesDefinition() {
        super(CommonAttributes.GLOBAL_MODULES, true, moduleValidator);
    }

    @Override
    public ModelNode validateResolvedOperation(ModelNode operationObject) throws OperationFailedException {
        final ModelNode result = super.validateOperation(operationObject);

        if (result.isDefined()) {
            for (ModelNode module : result.asList()) {
                if (!module.hasDefined(CommonAttributes.SLOT)) {
                    module.get(CommonAttributes.SLOT).set("main");
                }
            }
        }

        return result;
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(CommonAttributes.NAME, DESCRIPTION).set(bundle.getString("ee.global-modules.name"));
        valueType.get(CommonAttributes.SLOT, DESCRIPTION).set(bundle.getString("ee.global-modules.slot"));
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(CommonAttributes.NAME, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, CommonAttributes.NAME));
        valueType.get(CommonAttributes.SLOT, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, CommonAttributes.SLOT));
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);
        valueType.get(CommonAttributes.NAME, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, CommonAttributes.NAME));
        valueType.get(CommonAttributes.SLOT, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, CommonAttributes.SLOT));
    }

    @Override
    public void marshallAsElement(ModelNode eeSubSystem, XMLStreamWriter writer) throws XMLStreamException {
        if (eeSubSystem.hasDefined(getName()) && eeSubSystem.asInt() > 0) {
            writer.writeStartElement(Element.GLOBAL_MODULES.getLocalName());
            final ModelNode globalModules = eeSubSystem.get(getName());
            for (ModelNode module : globalModules.asList()) {
                writer.writeEmptyElement(Element.MODULE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), module.get(CommonAttributes.NAME).asString());
                if (module.hasDefined(CommonAttributes.SLOT)) {
                    writer.writeAttribute(Attribute.SLOT.getLocalName(), module.get(CommonAttributes.SLOT).asString());
                }
            }
            writer.writeEndElement();
        }
    }

    private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
        final ModelNode valueType = parent.get(VALUE_TYPE);
        final ModelNode name = valueType.get(CommonAttributes.NAME);
        name.get(DESCRIPTION); // placeholder
        name.get(TYPE).set(ModelType.STRING);
        name.get(NILLABLE).set(false);
        final ModelNode slot = valueType.get(CommonAttributes.SLOT);
        slot.get(DESCRIPTION);  // placeholder
        slot.get(TYPE).set(ModelType.STRING);
        slot.get(NILLABLE).set(true);

        return valueType;
    }
}
